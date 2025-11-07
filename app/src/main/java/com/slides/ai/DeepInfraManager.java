package com.slides.ai;

import android.os.Handler;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class DeepInfraManager {
    private static final String API_URL = "https://api.deepinfra.com/v1/openai/chat/completions";

    private Handler mainHandler;
    private ExecutorService executorService;
    private Gson gson = new Gson();

    public interface DeepInfraCallback<T> {
        void onSuccess(T response);
        void onError(String message);
    }

    public DeepInfraManager(Handler mainHandler, ExecutorService executorService) {
        this.mainHandler = mainHandler;
        this.executorService = executorService;
    }

    public void getCompletion(String prompt, String model, float canvasWidth, float canvasHeight, DeepInfraCallback<String> callback) {
        executorService.execute(() -> {
            try {
                URL url = new URL(API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                // Create the request body
                String enhancedPrompt = createSlideGenerationPrompt(prompt, canvasWidth, canvasHeight);
                DeepInfraRequest request = new DeepInfraRequest();
                request.model = model;
                request.messages = new ArrayList<>();
                request.messages.add(new DeepInfraRequest.Message("user", enhancedPrompt));

                String requestBody = gson.toJson(request);

                OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
                writer.write(requestBody);
                writer.flush();
                writer.close();

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    DeepInfraResponse response = gson.fromJson(reader, DeepInfraResponse.class);
                    reader.close();

                    if (response != null && response.choices != null && !response.choices.isEmpty()) {
                        mainHandler.post(() -> callback.onSuccess(response.choices.get(0).message.content));
                    } else {
                        mainHandler.post(() -> callback.onError("Received an empty response from the server."));
                    }
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

    // Inner classes for request and response
    private static class DeepInfraRequest {
        String model;
        List<Message> messages;

        static class Message {
            String role;
            String content;

            Message(String role, String content) {
                this.role = role;
                this.content = content;
            }
        }
    }

    private static class DeepInfraResponse {
        List<Choice> choices;

        static class Choice {
            Message message;
        }

        static class Message {
            String content;
        }
    }
}
