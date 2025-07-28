package com.slides.ai;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;

/**
* NetworkManager handles all API interactions and image loading operations
*/
public class NetworkManager {
	private final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";
	private ApiKeyManager apiKeyManager;
	private HashMap<String, Bitmap> imageCache;
	private Handler mainHandler;
	private Thread networkThread;
	private ExecutorService executorService;
	
	// Callback interface for API responses
	public interface ApiResponseCallback {
		void onSuccess(String jsonStr);
		void onError(String errorMessage);
	}
	
	// Callback interface for image loading
	public interface ImageLoadCallback {
		void onImageLoaded();
		void onImageLoadFailed(String errorMessage);
	}
	
	public NetworkManager(ApiKeyManager apiKeyManager, HashMap<String, Bitmap> imageCache,
	Handler mainHandler, ExecutorService executorService) {
		this.apiKeyManager = apiKeyManager;
		this.imageCache = imageCache;
		this.mainHandler = mainHandler;
		this.executorService = executorService;
	}
	
	public void sendPromptToGemini(String prompt, float canvasWidth, float canvasHeight, final ApiResponseCallback callback) {
		// Cancel any previous network operation
		if (networkThread != null && networkThread.isAlive()) {
			networkThread.interrupt();
		}
		
		networkThread = new Thread(new Runnable() {
			@Override
			public void run() {
				String result;
				try {
					// Construct the structured prompt with improved guidance for layout
					String structuredPrompt = "Create a professional presentation slide based on this prompt: \"" + prompt + "\". " +
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
					
					// Create JSON request body
					JSONObject requestBody = new JSONObject();
					JSONArray contents = new JSONArray();
					
					// Add the role and content
					JSONObject content = new JSONObject();
					content.put("role", "user");
					JSONObject part = new JSONObject();
					part.put("text", structuredPrompt);
					JSONArray parts = new JSONArray();
					parts.put(part);
					content.put("parts", parts);
					contents.put(content);
					
					requestBody.put("contents", contents);
					
					// Add generation config
					JSONObject generationConfig = new JSONObject();
					generationConfig.put("temperature", 0.7);
					generationConfig.put("topK", 40);
					generationConfig.put("topP", 0.95);
					generationConfig.put("maxOutputTokens", 8192);
					requestBody.put("generationConfig", generationConfig);
					
					// Get API key from manager
					String apiKey = apiKeyManager.getActiveApiKey();
					if (apiKey == null) {
						result = "ERROR: No API key available";
						return;
					}

					// Create URL with API key
					URL url = new URL(GEMINI_API_URL + "?key=" + apiKey);
					
					// Create connection
					HttpURLConnection connection = (HttpURLConnection) url.openConnection();
					connection.setRequestMethod("POST");
					connection.setRequestProperty("Content-Type", "application/json");
					connection.setDoOutput(true);
					
					// Write request body
					OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
					writer.write(requestBody.toString());
					writer.flush();
					writer.close();
					
					// Read response
					int responseCode = connection.getResponseCode();
					if (responseCode == HttpURLConnection.HTTP_OK) {
						BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
						StringBuilder response = new StringBuilder();
						String line;
						while ((line = reader.readLine()) != null) {
							response.append(line);
						}
						reader.close();
						
						result = response.toString();
					} else {
						BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
						StringBuilder response = new StringBuilder();
						String line;
						while ((line = reader.readLine()) != null) {
							response.append(line);
						}
						reader.close();
						
						result = "ERROR: " + responseCode + " - " + response.toString();
					}
					
					connection.disconnect();
				} catch (Exception e) {
					result = "ERROR: " + e.getMessage();
					e.printStackTrace();
				}
				
				// Process result on main thread
                final String finalResult = result;
                mainHandler.post(() -> {
                    if (finalResult != null && finalResult.startsWith("ERROR:")) {
                        callback.onError(finalResult);
                    } else if (finalResult != null) {
                        try {
                            // Parse the Gemini API response
                            JSONObject response = new JSONObject(finalResult);
                            JSONArray candidates = response.getJSONArray("candidates");
                            if (candidates.length() > 0) {
                                JSONObject firstCandidate = candidates.getJSONObject(0);
                                JSONObject content = firstCandidate.getJSONObject("content");
                                JSONArray parts = content.getJSONArray("parts");
                                if (parts.length() > 0) {
                                    JSONObject firstPart = parts.getJSONObject(0);
                                    String text = firstPart.getString("text");

                                    // Extract JSON from the response text
                                    String jsonStr = extractJsonFromResponse(text);
                                    callback.onSuccess(jsonStr);
                                } else {
                                    callback.onError("Empty parts in response");
                                }
                            } else {
                                callback.onError("No candidates in response");
                            }

                        } catch (Exception e) {
                            callback.onError("Error processing response: " + e.getMessage());
                            e.printStackTrace();
                        }
                    } else {
                        callback.onError("Empty response from server");
                    }
                });
			}
		});
		
		networkThread.start();
	}
	
	private String extractJsonFromResponse(String response) {
		// First try to find JSON wrapped in markdown code blocks
		if (response.contains("```json")) {
			int startIdx = response.indexOf("```json") + 7;
			int endIdx = response.lastIndexOf("```");
			if (endIdx > startIdx) {
				String jsonStr = response.substring(startIdx, endIdx).trim();
				if (isValidJson(jsonStr)) {
					return jsonStr;
				}
			}
		}

		// Try to find JSON wrapped in any code blocks
		if (response.contains("```")) {
			int startIdx = response.indexOf("```");
			int secondStart = response.indexOf('\n', startIdx);
			if (secondStart > startIdx) {
				int endIdx = response.lastIndexOf("```");
				if (endIdx > secondStart) {
					String jsonStr = response.substring(secondStart + 1, endIdx).trim();
					if (isValidJson(jsonStr)) {
						return jsonStr;
					}
				}
			}
		}

		// Find the first complete JSON object
		int startIdx = response.indexOf('{');
		if (startIdx != -1) {
			int braceCount = 0;
			int endIdx = startIdx;
			
			for (int i = startIdx; i < response.length(); i++) {
				char c = response.charAt(i);
				if (c == '{') {
					braceCount++;
				} else if (c == '}') {
					braceCount--;
					if (braceCount == 0) {
						endIdx = i;
						break;
					}
				}
			}
			
			if (braceCount == 0 && endIdx > startIdx) {
				String jsonStr = response.substring(startIdx, endIdx + 1);
				if (isValidJson(jsonStr)) {
					return jsonStr;
				}
			}
		}

		throw new IllegalArgumentException("No valid JSON found in response: " + response);
	}

	private boolean isValidJson(String jsonStr) {
		try {
			new JSONObject(jsonStr);
			return true;
		} catch (JSONException e) {
			return false;
		}
	}
	
	public void loadImage(String url, final ImageLoadCallback callback) {
		if (imageCache.containsKey(url)) {
			mainHandler.post(new Runnable() {
				@Override
				public void run() {
					callback.onImageLoaded();
				}
			});
			return;
		}
		
		executorService.execute(new Runnable() {
			@Override
			public void run() {
				try {
					// Download image
					URL imageUrl = new URL(url);
					HttpURLConnection connection = (HttpURLConnection) imageUrl.openConnection();
					connection.setDoInput(true);
					connection.connect();
					InputStream input = connection.getInputStream();
					final Bitmap bitmap = BitmapFactory.decodeStream(input);
					
					// Cache and refresh view on UI thread
					mainHandler.post(new Runnable() {
						@Override
						public void run() {
							imageCache.put(url, bitmap);
							callback.onImageLoaded();
						}
					});
				} catch (Exception e) {
					mainHandler.post(new Runnable() {
						@Override
						public void run() {
							callback.onImageLoadFailed(e.getMessage());
						}
					});
				}
			}
		});
	}
	
	public void cleanup() {
		if (networkThread != null && networkThread.isAlive()) {
			networkThread.interrupt();
		}
	}
}
