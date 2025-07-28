package com.slides.ai;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import android.graphics.Bitmap;
import java.util.HashMap;


/**
* ElementFactory handles the creation of slide elements from JSON data
*/
public class ElementFactory {
	
	/**
	* Creates a list of slide elements from JSON data
	* 
	* @param slideData The JSON object containing slide data
	* @param context The application context
	* @return List of SlideElement objects
	*/
	public static List<SlideElement> createElementsFromJSON(JSONObject slideData, Context context, HashMap<String, Bitmap> imageCache) {
		List<SlideElement> elements = new ArrayList<>();
		Log.d("ElementFactory", "Starting JSON parsing. Data: " + slideData.toString());

		try {
			if (!slideData.has("elements")) {
				Log.e("ElementFactory", "JSON data does not contain 'elements' array.");
				return elements;
			}
			// Parse elements
			JSONArray jsonElements = slideData.getJSONArray("elements");
			Log.d("ElementFactory", "Found " + jsonElements.length() + " elements in JSON.");

			for (int i = 0; i < jsonElements.length(); i++) {
				JSONObject element = jsonElements.getJSONObject(i);
				String type = element.optString("type", "unknown");

				Log.d("ElementFactory", "Processing element " + i + ": " + element.toString());
				Log.d("ElementFactory", "Creating element type: " + type + " at position (" +
					element.optInt("x", 0) + "," + element.optInt("y", 0) + ")");

				try {
					SlideElement newElement = null;
					switch (type.toLowerCase()) {
						case "text":
							newElement = new TextElement(element, context);
							Log.i("ElementFactory", "Successfully created TextElement: " + element.optString("content", ""));
							break;
						case "image":
							newElement = new ImageElement(element, context);
							Log.i("ElementFactory", "Successfully created ImageElement");
							break;
						case "shape":
							newElement = new ShapeElement(element, context);
							Log.i("ElementFactory", "Successfully created ShapeElement: " + element.optString("shapeType", ""));
							break;
						// Handle Qwen format - convert rectangle to shape
						case "rectangle":
							element.put("type", "shape");
							element.put("shapeType", "rectangle");
							if (element.optInt("height", 0) < 2) {
								element.put("height", 2);
							}
							newElement = new ShapeElement(element, context);
							Log.i("ElementFactory", "Successfully created ShapeElement from rectangle");
							break;
						// Handle Qwen format - convert oval to shape
						case "oval":
							element.put("type", "shape");
							element.put("shapeType", "oval");
							if (element.optInt("width", 0) < 2) {
								element.put("width", 2);
							}
							if (element.optInt("height", 0) < 2) {
								element.put("height", 2);
							}
							newElement = new ShapeElement(element, context);
							Log.i("ElementFactory", "Successfully created ShapeElement from oval");
							break;
						case "circle":
							element.put("type", "shape");
							element.put("shapeType", "oval");
							newElement = new ShapeElement(element, context);
							Log.i("ElementFactory", "Successfully created ShapeElement from circle");
							break;
						case "line":
							element.put("type", "shape");
							element.put("shapeType", "line");
							newElement = new ShapeElement(element, context);
							Log.i("ElementFactory", "Successfully created ShapeElement from line");
							break;
						case "triangle":
							element.put("type", "shape");
							element.put("shapeType", "triangle");
							newElement = new ShapeElement(element, context);
							Log.i("ElementFactory", "Successfully created ShapeElement from triangle");
							break;
						case "table":
							newElement = new TableElement(element, context);
							Log.i("ElementFactory", "Successfully created TableElement");
							break;
						case "chart":
							newElement = new ChartElement(element, context);
							Log.i("ElementFactory", "Successfully created ChartElement");
							break;
						case "icon":
							newElement = new IconElement(element, context);
							Log.i("ElementFactory", "Successfully created IconElement");
							break;
						default:
							Log.w("ElementFactory", "Unknown element type: " + type + ". Creating a fallback error element.");
							newElement = createErrorTextElement(context, "Unknown type: " + type);
							break;
					}
					if (newElement != null) {
						elements.add(newElement);
					}
				} catch (Exception e) {
					Log.e("ElementFactory", "Error creating element of type " + type, e);
					SlideElement errorElement = createErrorTextElement(context, "Error parsing " + type);
					if (errorElement != null) {
						elements.add(errorElement);
					}
				}
			}
		} catch (Exception e) {
			Log.e("ElementFactory", "Error creating elements from JSON", e);
		}

		Log.d("ElementFactory", "Successfully created " + elements.size() + " elements from JSON");
		return elements;
	}

	private static TextElement createErrorTextElement(Context context, String errorMessage) {
		try {
			JSONObject json = new JSONObject();
			json.put("type", "text");
			json.put("content", "Error: " + errorMessage);
			json.put("x", 10);
			json.put("y", 10);
			json.put("width", 280);
			json.put("height", 180);
			json.put("fontSize", 12);
			json.put("color", "#FF0000");
			json.put("bold", true);
			json.put("alignment", "center");

			return new TextElement(json, context);
		} catch (JSONException e) {
			Log.e("ElementFactory", "Error creating default error text element", e);
			return null; // Should not happen
		}
	}
	
	/**
	* Creates a single element from JSON data
	* 
	* @param elementData The JSON object containing element data
	* @param context The application context
	* @return A SlideElement object
	* @throws JSONException If JSON parsing fails
	*/
	public static SlideElement createElement(JSONObject elementData, Context context, HashMap<String, Bitmap> imageCache) throws JSONException {
		String type = elementData.getString("type");
		
		switch (type.toLowerCase()) {
			case "text":
			return new TextElement(elementData, context);
			case "image":
			return new ImageElement(elementData, context);
			case "shape":
			return new ShapeElement(elementData, context);
			case "table":
			return new TableElement(elementData, context);
			default:
			throw new IllegalArgumentException("Unknown element type: " + type);
		}
	}
	
	/**
	* Creates a default text element
	* 
	* @param context The application context
	* @return A TextElement with default properties
	*/
	public static TextElement createDefaultTextElement(Context context) {
		try {
			JSONObject json = new JSONObject();
			json.put("type", "text");
			json.put("content", "New Text");
			json.put("x", 50);
			json.put("y", 50);
			json.put("width", 200);
			json.put("height", 50);
			json.put("fontSize", 16);
			json.put("color", "#000000");
			json.put("bold", false);
			json.put("italic", false);
			json.put("alignment", "left");
			
			return new TextElement(json, context);
		} catch (JSONException e) {
			Log.e("ElementFactory", "Error creating default text element", e);
			return null;
		}
	}
	
	/**
	* Creates a default shape element
	* 
	* @param context The application context
	* @return A ShapeElement with default properties
	*/
	public static ShapeElement createDefaultShapeElement(Context context) {
		try {
			JSONObject json = new JSONObject();
			json.put("type", "shape");
			json.put("shapeType", "rectangle");
			json.put("x", 50);
			json.put("y", 50);
			json.put("width", 100);
			json.put("height", 100);
			json.put("color", "#2196F3");
			json.put("cornerRadius", 8);
			json.put("opacity", 1.0);
			json.put("strokeWidth", 0);
			json.put("strokeColor", "#000000");
			
			return new ShapeElement(json, context);
		} catch (JSONException e) {
			Log.e("ElementFactory", "Error creating default shape element", e);
			return null;
		}
	}
	
	/**
	* Creates a default image element
	* 
	* @param context The application context
	* @return An ImageElement with default properties
	*/
	public static ImageElement createDefaultImageElement(Context context) {
		try {
			JSONObject json = new JSONObject();
			json.put("type", "image");
			json.put("url", "https://via.placeholder.com/150");
			json.put("x", 50);
			json.put("y", 50);
			json.put("width", 150);
			json.put("height", 150);
			json.put("cornerRadius", 0);
			
			return new ImageElement(json, context);
		} catch (JSONException e) {
			Log.e("ElementFactory", "Error creating default image element", e);
			return null;
		}
	}
	
	public static IconElement createDefaultIconElement(Context context) {
		try {
			JSONObject json = new JSONObject();
			json.put("type", "icon");
			json.put("iconName", "settings");
			json.put("color", "#2196F3");
			json.put("x", 50);
			json.put("y", 50);
			json.put("width", 40);
			json.put("height", 40);
			
			return new IconElement(json, context);
		} catch (Exception e) {
			Log.e("ElementFactory", "Error creating icon", e);
			return null;
		}
	}
	
	public static ChartElement createDefaultChartElement(Context context) {
		try {
			JSONObject json = new JSONObject();
			json.put("type", "chart");
			json.put("chartType", "bar");
			json.put("showLegend", true);
			
			JSONArray data = new JSONArray();
			for (int i = 0; i < 3; i++) {
				JSONObject item = new JSONObject();
				item.put("label", "Item " + (i+1));
				item.put("value", (i+1) * 10);
				item.put("color", i == 0 ? "#FF5722" : i == 1 ? "#4CAF50" : "#2196F3");
				data.put(item);
			}
			json.put("data", data);
			
			return new ChartElement(json, context);
		} catch (Exception e) {
			Log.e("ElementFactory", "Error creating chart", e);
			return null;
		}
	}
	
	/**
	* Creates a default table element
	* 
	* @param context The application context
	* @return A TableElement with default properties
	*/
	public static TableElement createDefaultTableElement(Context context) {
		try {
			JSONObject json = new JSONObject();
			json.put("type", "table");
			json.put("x", 50);
			json.put("y", 50);
			json.put("width", 200);
			json.put("height", 100);
			json.put("rows", 3);
			json.put("columns", 3);
			
			// Create default data
			JSONArray data = new JSONArray();
			for (int i = 0; i < 3; i++) {
				JSONArray row = new JSONArray();
				for (int j = 0; j < 3; j++) {
					row.put("Cell " + i + "," + j);
				}
				data.put(row);
			}
			json.put("data", data);
			
			json.put("headerColor", "#E3F2FD");
			json.put("cellColor", "#FFFFFF");
			json.put("borderColor", "#2196F3");
			json.put("borderWidth", 1);
			
			return new TableElement(json, context);
		} catch (JSONException e) {
			Log.e("ElementFactory", "Error creating default table element", e);
			return null;
		}
	}
}
