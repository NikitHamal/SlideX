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
                URL url = new URL(NEW_CHAT_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjhiYjQ1NjVmLTk3NjUtNDQwNi04OWQ5LTI3NmExMTIxMjBkNiIsImxhc3RfcGFzc3dvcmRfY2hhbmdlIjoxNzUwNjYwODczLCJleHAiOjE3NTU4NDg1NDh9.pb0IybY9tQkriqMUOos72FKtZM3G4p1_aDzwqqh5zX4");
                connection.setDoOutput(true);

                List<String> models = new ArrayList<>();
                models.add("qwen3-235b-a22b");
                QwenNewChatRequest request = new QwenNewChatRequest("New Chat", models, "normal", "t2t", System.currentTimeMillis());
                String requestBody = gson.toJson(request);

                OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
                writer.write(requestBody);
                writer.flush();
                writer.close();

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    QwenNewChatResponse response = gson.fromJson(reader, QwenNewChatResponse.class);
                    mainHandler.post(() -> callback.onSuccess(response));
                    reader.close();
                } else {
                    mainHandler.post(() -> callback.onError("Error: " + responseCode));
                }
                connection.disconnect();
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void getCompletion(String chatId, String parentId, String prompt, String model, QwenCallback<String> callback) {
        executorService.execute(() -> {
            try {
                URL url = new URL(COMPLETION_URL + chatId);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjhiYjQ1NjVmLTk3NjUtNDQwNi04OWQ5LTI3NmExMTIxMjBkNiIsImxhc3RfcGFzc3dvcmRfY2hhbmdlIjoxNzUwNjYwODczLCJleHAiOjE3NTU4NDg1NDh9.pb0IybY9tQkriqMUOos72FKtZM3G4p1_aDzwqqh5zX4");
                connection.setDoOutput(true);

                QwenCompletionRequest request = new QwenCompletionRequest();
                request.stream = true;
                request.incremental_output = true;
                request.chat_id = chatId;
                request.chat_mode = "normal";
                request.model = model;
                request.parent_id = parentId;
                request.messages = new ArrayList<>();

                QwenCompletionRequest.Message message = new QwenCompletionRequest.Message();
                message.role = "user";
                message.content = prompt;
                message.timestamp = System.currentTimeMillis();
                message.chat_type = "t2t";
                message.sub_chat_type = "t2t";
                message.user_action = "chat";
                message.feature_config = new QwenCompletionRequest.FeatureConfig();
                message.feature_config.thinking_enabled = false;
                message.feature_config.output_schema = "phase";
                message.extra = new QwenCompletionRequest.Extra();
                message.extra.meta = new QwenCompletionRequest.Meta();
                message.extra.meta.subChatType = "t2t";
                message.parent_id = parentId;


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
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data:")) {
                            String json = line.substring(5).trim();
                            if (!json.isEmpty()) {
                                QwenCompletionResponse response = gson.fromJson(json, QwenCompletionResponse.class);
                                if (response != null && response.choices != null && !response.choices.isEmpty()) {
                                    QwenCompletionResponse.Delta delta = response.choices.get(0).delta;
                                    if (delta != null && delta.content != null) {
                                        fullResponse.append(delta.content);
                                    }
                                }
                            }
                        }
                    }
                    mainHandler.post(() -> callback.onSuccess(fullResponse.toString()));
                    reader.close();
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
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }
}
