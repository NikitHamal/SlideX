package com.slides.ai.qwen;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.slides.ai.qwen.auth.TokenManager;
import com.slides.ai.qwen.models.ChatCompletionRequest;
import com.slides.ai.qwen.models.ChatCompletionResponse;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

public class QwenManager {
    private static final String DEFAULT_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";

    private final TokenManager tokenManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Gson gson = new Gson();
    private final OkHttpClient httpClient;

    private List<ChatCompletionRequest.Message> conversationHistory = new ArrayList<>();

    public interface QwenCallback<T> {
        void onSuccess(T response);
        void onError(String message);
    }

    public QwenManager(Context context) {
        this.tokenManager = new TokenManager(context);
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public void getCompletion(String prompt, String model, float canvasWidth, float canvasHeight, QwenCallback<String> callback) {
        tokenManager.getValidAccessToken(new TokenManager.TokenCallback() {
            @Override
            public void onTokenAvailable(String accessToken) {
                executeCompletionRequest(accessToken, prompt, model, canvasWidth, canvasHeight, callback);
            }

            @Override
            public void onError(String message) {
                mainHandler.post(() -> callback.onError(message));
            }
        });
    }

    private void executeCompletionRequest(String accessToken, String prompt, String model, float canvasWidth, float canvasHeight, QwenCallback<String> callback) {
        String baseUrl = getApiBaseUrl();
        String url = baseUrl + "/chat/completions";

        String enhancedPrompt = createSlideGenerationPrompt(prompt, canvasWidth, canvasHeight);

        List<ChatCompletionRequest.Message> messages = new ArrayList<>(conversationHistory);
        messages.add(new ChatCompletionRequest.Message("user", enhancedPrompt));

        ChatCompletionRequest request = new ChatCompletionRequest(model, messages, true);
        String requestBody = gson.toJson(request);

        Request httpRequest = new Request.Builder()
                .url(url)
                .post(RequestBody.create(requestBody, MediaType.get("application/json")))
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Accept", "text/event-stream")
                .build();

        EventSourceListener listener = new EventSourceListener() {
            private final StringBuilder fullResponse = new StringBuilder();

            @Override
            public void onEvent(EventSource eventSource, String id, String type, String data) {
                if ("[DONE]".equals(data)) {
                    return;
                }
                try {
                    ChatCompletionResponse response = gson.fromJson(data, ChatCompletionResponse.class);
                    if (response != null && response.choices != null && !response.choices.isEmpty()) {
                        String content = response.choices.get(0).delta.content;
                        if (content != null) {
                            fullResponse.append(content);
                        }
                    }
                } catch (Exception e) {
                   // Ignore malformed JSON
                }
            }

            @Override
            public void onClosed(EventSource eventSource) {
                String finalResponse = fullResponse.toString();
                // Add user and AI messages to history
                conversationHistory.add(new ChatCompletionRequest.Message("user", enhancedPrompt));
                conversationHistory.add(new ChatCompletionRequest.Message("assistant", finalResponse));

                // Keep history manageable
                if (conversationHistory.size() > 10) {
                    conversationHistory = conversationHistory.subList(conversationHistory.size() - 10, conversationHistory.size());
                }

                mainHandler.post(() -> callback.onSuccess(finalResponse));
            }

            @Override
            public void onFailure(EventSource eventSource, Throwable t, Response response) {
                String errorMessage = "SSE Error";
                if (t != null) {
                    errorMessage += ": " + t.getMessage();
                } else if (response != null) {
                     try {
                        errorMessage += " " + response.code() + ": " + response.body().string();
                    } catch (IOException e) {
                         errorMessage += " " + response.code();
                    }
                }
                final String finalErrorMessage = errorMessage;
                mainHandler.post(() -> callback.onError(finalErrorMessage));
            }
        };

       EventSources.createFactory(httpClient).newEventSource(httpRequest, listener);
    }

    private String getApiBaseUrl() {
        String resource = tokenManager.getToken() != null ? tokenManager.getToken().resource : null;
        String url = resource != null ? resource : DEFAULT_BASE_URL;
        if (!url.startsWith("http")) {
            url = "https://" + url;
        }
        if (!url.endsWith("/v1")) {
            url = url.replaceAll("/?$", "/v1"); // Add /v1 if missing
        }
        return url;
    }

    private String createSlideGenerationPrompt(String userPrompt, float canvasWidth, float canvasHeight) {
         return "Create a professional presentation slide based on this request: \"" + userPrompt + "\". " +
                "The canvas size is " + canvasWidth + "x" + canvasHeight + " pixels. Please generate the slide elements accordingly." +
                "You must respond with ONLY a valid JSON object (no markdown, no explanation) that contains:\n" +
                "{\n" +
                "  \"backgroundColor\": \"#FFFFFF\",\n" +
                "  \"elements\": [\n" +
                "    {\n" +
                "      \"type\": \"text\",\n" +
                "      \"content\": \"Slide Title\",\n" +
                "      \"x\": 20,\n" +
                "      \"y\": 20,\n" +
                "      \"width\": 280,\n" +
                "      \"height\": 40,\n" +
                "      \"fontSize\": 24,\n" +
                "      \"color\": \"#000000\",\n" +
                "      \"bold\": true,\n" +
                "      \"alignment\": \"center\"\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "Guidelines:\n" +
                "- Use slide dimensions " + canvasWidth + "x" + canvasHeight + "dp\n" +
                "- Position elements with proper spacing\n" +
                "- Include title, content, and optionally images/shapes\n" +
                "- Use readable fonts (fontSize 12-24)\n" +
                "- Choose professional colors\n" +
                "- For images, use real public URLs\n" +
                "- For shapes: type can be 'rectangle', 'oval', 'line'\n" +
                "- Ensure no element overlap\n" +
                "IMPORTANT: Return ONLY the JSON object, nothing else.";
    }

    public void clearConversation() {
        conversationHistory.clear();
    }
}
