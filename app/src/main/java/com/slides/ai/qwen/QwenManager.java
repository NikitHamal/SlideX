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
                // Get API key from manager - using Qwen token
                String qwenToken = apiKeyManager.getQwenToken();
                if (qwenToken == null || qwenToken.isEmpty()) {
                    mainHandler.post(() -> callback.onError("No Qwen API token available. Please add it in Settings."));
                    return;
                }

                URL url = new URL(NEW_CHAT_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + qwenToken);
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

    public void getCompletion(String chatId, String parentId, String prompt, String model, float canvasWidth, float canvasHeight, QwenCallback<String> callback) {
        executorService.execute(() -> {
            try {
                // Get API key from manager
                String qwenToken = apiKeyManager.getQwenToken();
                if (qwenToken == null || qwenToken.isEmpty()) {
                    mainHandler.post(() -> callback.onError("No Qwen API token available. Please add it in Settings."));
                    return;
                }

                // Use stored chat ID if available, otherwise use provided one
                String activeChatId = currentChatId != null ? currentChatId : chatId;

                URL url = new URL(COMPLETION_URL + activeChatId);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + qwenToken);
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
                    String responseId = null;
                    
                    while ((line = reader.readLine()) != null) {
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
                mainHandler.post(() -> callback.onError("Request error: " + e.getMessage()));
            }
        });
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
        currentChatId = null;
        lastParentId = null;
        conversationHistory.clear();
    }
}
