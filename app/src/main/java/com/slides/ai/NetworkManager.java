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
	private final String API_KEY;
	private final String GEMINI_API_URL;
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
	
	public NetworkManager(String apiKey, String apiUrl, HashMap<String, Bitmap> imageCache, 
	Handler mainHandler, ExecutorService executorService) {
		this.API_KEY = apiKey;
		this.GEMINI_API_URL = apiUrl;
		this.imageCache = imageCache;
		this.mainHandler = mainHandler;
		this.executorService = executorService;
	}
	
	public void sendPromptToGemini(String prompt, final ApiResponseCallback callback) {
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
					"Respond with a JSON object containing these fields:\n" +
					"- backgroundColor: hex color code for slide background (use professional, subtle colors)\n" +
					"- elements: array of objects, each with:\n" +
					"  - type: 'text', 'image', 'shape', or 'table'\n" +
					"  - for text: content, x, y, width, height, fontSize, color, bold (boolean), italic (boolean), alignment (string: 'left', 'center', 'right')\n" +
					"  - for image: url, x, y, width, height, cornerRadius (optional)\n" +
					"  - for shape: shapeType ('rectangle', 'oval', 'line', 'triangle', 'star', 'hexagon'), x, y, width, height, color, cornerRadius (for rectangles), opacity (0.0-1.0), strokeWidth (optional), strokeColor (optional)\n" +
					"  - for table: rows, columns, x, y, width, height, data (2D array), headerColor, cellColor\n" +
					"For charts: include chartType ('bar', 'pie', 'line'), data array with value, label, color\n" +
					"For icons: include iconName ('home', 'settings', etc.) and color" +
					"Be creative but focused. Use real image URLs. All positions (x,y) and dimensions should be in dp relative to the slide size (320x200).\n" +
					"IMPORTANT: Ensure elements don't overlap and maintain proper spacing. Place title at top, ensure text is readable, and create a balanced layout with clear visual hierarchy.";
					
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
					
					// Create URL with API key
					URL url = new URL(GEMINI_API_URL + "?key=" + API_KEY);
					
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
				mainHandler.post(new Runnable() {
					@Override
					public void run() {
						if (finalResult.startsWith("ERROR:")) {
							callback.onError(finalResult);
							return;
						}
						
						try {
							// Parse the Gemini API response
							JSONObject response = new JSONObject(finalResult);
							JSONArray candidates = response.getJSONArray("candidates");
							JSONObject firstCandidate = candidates.getJSONObject(0);
							JSONObject content = firstCandidate.getJSONObject("content");
							JSONArray parts = content.getJSONArray("parts");
							JSONObject firstPart = parts.getJSONObject(0);
							String text = firstPart.getString("text");
							
							// Extract JSON from the response text
							String jsonStr = extractJsonFromResponse(text);
							callback.onSuccess(jsonStr);
							
						} catch (Exception e) {
							callback.onError("Error processing response: " + e.getMessage());
							e.printStackTrace();
						}
					}
				});
			}
		});
		
		networkThread.start();
	}
	
	private String extractJsonFromResponse(String response) {
		// Find the first { and the last } to extract JSON
		int startIdx = response.indexOf('{');
		int endIdx = response.lastIndexOf('}');
		
		if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
			return response.substring(startIdx, endIdx + 1);
		} else {
			throw new IllegalArgumentException("No valid JSON found in response");
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
