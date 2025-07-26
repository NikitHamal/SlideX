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
		
		try {
			// Parse elements
			JSONArray jsonElements = slideData.getJSONArray("elements");
			for (int i = 0; i < jsonElements.length(); i++) {
				JSONObject element = jsonElements.getJSONObject(i);
				String type = element.getString("type");
				
				switch (type.toLowerCase()) {
					case "text":
					elements.add(new TextElement(element, context));
					break;
					case "image":
					elements.add(new ImageElement(element, context, imageCache));
					break;
					case "shape":
					elements.add(new ShapeElement(element, context));
					break;
					case "table":
					elements.add(new TableElement(element, context));
					break;
					case "chart":
					elements.add(new ChartElement(element, context));
					break;
					case "icon":
					elements.add(new IconElement(element, context));
					break;
				}
			}
		} catch (Exception e) {
			Log.e("ElementFactory", "Error creating elements from JSON", e);
		}
		
		return elements;
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
			return new ImageElement(elementData, context, imageCache);
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
	public static ImageElement createDefaultImageElement(Context context, HashMap<String, Bitmap> imageCache) {
		try {
			JSONObject json = new JSONObject();
			json.put("type", "image");
			json.put("url", "https://via.placeholder.com/150");
			json.put("x", 50);
			json.put("y", 50);
			json.put("width", 150);
			json.put("height", 150);
			json.put("cornerRadius", 0);
			
			return new ImageElement(json, context, imageCache);
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
