package com.slides.ai.qwen;

import android.os.Handler;
import com.google.gson.Gson;
import com.slides.ai.ApiKeyManager;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class QwenManager {
    private static final String NEW_CHAT_URL = "https://chat.qwen.ai/api/v2/chats/new";
    private static final String COMPLETION_URL = "https://chat.qwen.ai/api/v2/chat/completions?chat_id=";

    private ApiKeyManager apiKeyManager;
    private Handler mainHandler;
    private ExecutorService executorService;
    private Gson gson = new Gson();

    // Store conversation context
    private String currentChatId;
    private String lastParentId;
    private List<QwenCompletionRequest.Message> conversationHistory = new ArrayList<>();
    private String cookies;

    public interface QwenCallback<T> {
        void onSuccess(T response);
        void onError(String message);
    }

    public QwenManager(ApiKeyManager apiKeyManager, Handler mainHandler, ExecutorService executorService) {
        this.apiKeyManager = apiKeyManager;
        this.mainHandler = mainHandler;
        this.executorService = executorService;
    }

    public void createNewChat(QwenCallback<QwenNewChatResponse> callback) {
        executorService.execute(() -> {
            try {
                if (midtoken == null || midtokenUses >= 5) {
                    fetchMidtoken(callback);
                    if (midtoken == null) {
                        return; // fetchMidtoken will post error
                    }
                } else {
                    midtokenUses++;
                }

                URL url = new URL(NEW_CHAT_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36");
                connection.setRequestProperty("Referer", "https://chat.qwen.ai/");
                connection.setRequestProperty("bx-v", "2.5.31");
                connection.setRequestProperty("Source", "web");
                connection.setRequestProperty("Authorization", "Bearer");
                connection.setRequestProperty("bx-umidtoken", midtoken);
                if (cookies != null) {
                    connection.setRequestProperty("Cookie", cookies);
                }
                connection.setDoOutput(true);

                List<String> models = new ArrayList<>();
                models.add("qwen3-235b-a22b");
                QwenNewChatRequest request = new QwenNewChatRequest("AI Slide Generator", models, "normal", "t2t", System.currentTimeMillis());
                String requestBody = gson.toJson(request);

                OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
                writer.write(requestBody);
                writer.flush();
                writer.close();

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    QwenNewChatResponse response = gson.fromJson(reader, QwenNewChatResponse.class);

                    // Store chat ID for conversation context
                    if (response.success && response.data != null) {
                        currentChatId = response.data.id;
                        lastParentId = null;
                        conversationHistory.clear();

                        // Store cookies
                        List<String> cookieList = connection.getHeaderFields().get("Set-Cookie");
                        if (cookieList != null) {
                            cookies = String.join(";", cookieList);
                        }
                    }

                    mainHandler.post(() -> callback.onSuccess(response));
                    reader.close();
                } else {
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    StringBuilder errorResponse = new StringBuilder();
                    String errorLine;
                    while ((errorLine = errorReader.readLine()) != null) {
                        errorResponse.append(errorLine);
                    }
                    errorReader.close();
                    mainHandler.post(() -> callback.onError("Error: " + responseCode + " - " + errorResponse.toString()));
                }
                connection.disconnect();
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Connection error: " + e.getMessage()));
            }
        });
    }

    private String midtoken;
    private int midtokenUses = 0;

    private void fetchMidtoken(QwenCallback<?> callback) {
        try {
            URL url = new URL("https://sg-wum.alibaba.com/w/wu.json");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Extract midtoken using regex
                String responseString = response.toString();
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?:umx\\.wu|__fycb)\\('([^']+)'\\)");
                java.util.regex.Matcher matcher = pattern.matcher(responseString);
                if (matcher.find()) {
                    midtoken = matcher.group(1);
                    midtokenUses = 1;
                } else {
                    mainHandler.post(() -> callback.onError("Failed to extract midtoken."));
                }
            } else {
                mainHandler.post(() -> callback.onError("Failed to fetch midtoken: " + responseCode));
            }
            connection.disconnect();
        } catch (Exception e) {
            mainHandler.post(() -> callback.onError("Failed to fetch midtoken: " + e.getMessage()));
        }
    }

    public void getCompletion(String chatId, String parentId, String prompt, String model, float canvasWidth, float canvasHeight, QwenCallback<String> callback) {
        executorService.execute(() -> {
            try {
                if (midtoken == null || midtokenUses >= 5) {
                    fetchMidtoken(callback);
                    if (midtoken == null) {
                        return; // fetchMidtoken will post error
                    }
                } else {
                    midtokenUses++;
                }

                // Use stored chat ID if available, otherwise use provided one
                String activeChatId = currentChatId != null ? currentChatId : chatId;

                URL url = new URL(COMPLETION_URL + activeChatId);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36");
                connection.setRequestProperty("Referer", "https://chat.qwen.ai/");
                connection.setRequestProperty("bx-v", "2.5.31");
                connection.setRequestProperty("Source", "web");
                connection.setRequestProperty("Authorization", "Bearer");
                connection.setRequestProperty("bx-umidtoken", midtoken);
                if (cookies != null) {
                    connection.setRequestProperty("Cookie", cookies);
                }
                connection.setDoOutput(true);

                QwenCompletionRequest request = new QwenCompletionRequest();
                request.stream = true;
                request.incremental_output = true;
                request.chat_id = activeChatId;
                request.chat_mode = "normal";
                request.model = model;
                request.parent_id = lastParentId;
                request.messages = new ArrayList<>();

                // Add conversation history to maintain context
                request.messages.addAll(conversationHistory);

                // Create enhanced prompt for slide generation
                String enhancedPrompt = createSlideGenerationPrompt(prompt, canvasWidth, canvasHeight);

                QwenCompletionRequest.Message message = new QwenCompletionRequest.Message();
                message.role = "user";
                message.content = enhancedPrompt;
                message.timestamp = System.currentTimeMillis();
                message.chat_type = "t2t";
                message.sub_chat_type = "t2t";
                message.user_action = "chat";
                message.feature_config = new QwenCompletionRequest.FeatureConfig();
                message.feature_config.thinking_enabled = false; // Disabled as requested
                message.feature_config.output_schema = "phase";
                message.extra = new QwenCompletionRequest.Extra();
                message.extra.meta = new QwenCompletionRequest.Meta();
                message.extra.meta.subChatType = "t2t";
                message.parent_id = lastParentId;

                request.messages.add(message);
                request.timestamp = System.currentTimeMillis();

                String requestBody = gson.toJson(request);

                OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
                writer.write(requestBody);
                writer.flush();
                writer.close();

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String line;
                    StringBuilder fullResponse = new StringBuilder();
                    StringBuilder rawResponse = new StringBuilder();
                    String responseId = null;

                    while ((line = reader.readLine()) != null) {
                        rawResponse.append(line).append("\n");
                        if (line.startsWith("data:")) {
                            String json = line.substring(5).trim();
                            if (!json.isEmpty() && !json.equals("[DONE]")) {
                                try {
                                    QwenCompletionResponse response = gson.fromJson(json, QwenCompletionResponse.class);
                                    if (response != null) {
                                        // Store response ID for conversation context
                                        if (response.response_created != null && response.response_created.response_id != null) {
                                            responseId = response.response_created.response_id;
                                        }

                                        if (response.choices != null && !response.choices.isEmpty()) {
                                            QwenCompletionResponse.Delta delta = response.choices.get(0).delta;
                                            if (delta != null && delta.content != null &&
                                                "answer".equals(delta.phase)) { // Only collect answer phase content
                                                fullResponse.append(delta.content);
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    // Skip malformed JSON lines
                                }
                            }
                        }
                    }
                    reader.close();

                    if (fullResponse.length() == 0 && rawResponse.length() > 0) {
                        mainHandler.post(() -> callback.onError("Received an unparsable response from the server:\n\n" + rawResponse.toString()));
                        return;
                    }

                    // Update conversation context
                    if (responseId != null) {
                        lastParentId = responseId;

                        // Add user message to history
                        conversationHistory.add(message);

                        // Add AI response to history
                        QwenCompletionRequest.Message aiMessage = new QwenCompletionRequest.Message();
                        aiMessage.role = "assistant";
                        aiMessage.content = fullResponse.toString();
                        aiMessage.timestamp = System.currentTimeMillis();
                        aiMessage.parent_id = lastParentId;
                        conversationHistory.add(aiMessage);

                        // Keep conversation history manageable (last 10 messages)
                        if (conversationHistory.size() > 10) {
                            conversationHistory = conversationHistory.subList(
                                conversationHistory.size() - 10, conversationHistory.size());
                        }
                    }

                    mainHandler.post(() -> callback.onSuccess(fullResponse.toString()));
                } else {
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    StringBuilder errorResponse = new StringBuilder();
                    String errorLine;
                    while ((errorLine = errorReader.readLine()) != null) {
                        errorResponse.append(errorLine);
                    }
                    errorReader.close();
                    mainHandler.post(() -> callback.onError("Error: " + responseCode + " " + errorResponse.toString()));
                }
                connection.disconnect();
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Request error: " + e.getMessage()));
            }
        });
    }

    private String createSlideGenerationPrompt(String userPrompt, float canvasWidth, float canvasHeight) {
        return "Create a professional presentation slide based on this request: \"" + userPrompt + "\". " +
                "You must respond with ONLY a valid HTML `<section>` element for a reveal.js presentation (no markdown, no explanation). Example:\n" +
                "<section>\n" +
                "  <h2>Slide Title</h2>\n" +
                "  <p>Slide content</p>\n" +
                "</section>\n" +
                "Guidelines:\n" +
                "- Use standard HTML tags like `<h2>`, `<p>`, `<ul>`, `<li>`, `<img>`.\n" +
                "- For images, use real public URLs.\n" +
                "- You can use reveal.js fragments to animate elements, for example: `<p class=\"fragment\">This will fade in</p>`.\n" +
                "IMPORTANT: Return ONLY the HTML `<section>` element, nothing else.";
    }

    public void clearConversation() {
        currentChatId = null;
        lastParentId = null;
        conversationHistory.clear();
    }
}
