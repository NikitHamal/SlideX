package com.slides.ai;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import org.json.JSONException;
import org.json.JSONObject;

public class IconElement extends SlideElement {
	private String iconName; // e.g., "home", "settings"
	private int iconColor;
	private Paint iconPaint;
	private Typeface iconFont;
	
	public IconElement(JSONObject json, Context context) throws JSONException {
		super(json, context);
		iconName = json.getString("iconName");
		iconColor = Color.parseColor(json.optString("color", "#000000"));
		
		try {
			iconFont = Typeface.createFromAsset(context.getAssets(), "material_icons.ttf");
		} catch (Exception e) {
			iconFont = Typeface.DEFAULT;
		}
		
		initPaint();
	}
	
	private void initPaint() {
		iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		iconPaint.setColor(iconColor);
		iconPaint.setTextSize(Math.min(width, height));
		iconPaint.setTypeface(iconFont);
		iconPaint.setTextAlign(Paint.Align.CENTER);
	}
	
	@Override
	public void draw(Canvas canvas) {
		canvas.save();
		canvas.translate(x, y);
		
		// Get icon character from Material Icons font
		String iconChar = getIconChar(iconName);
		
		// Center the icon
		float xPos = width / 2f;
		float yPos = (height / 2f) - ((iconPaint.descent() + iconPaint.ascent()) / 2f);
		
		canvas.drawText(iconChar, xPos, yPos, iconPaint);
		canvas.restore();
	}
    
    @Override
	public JSONObject toJson() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("type", "icon");
		json.put("x", x);
		json.put("y", y);
		json.put("width", width);
		json.put("height", height);
		json.put("iconName", iconName);
		json.put("color", String.format("#%06X", (0xFFFFFF & iconColor)));
		return json;
	}
	
	public String getIconName() { return iconName; }
	public void setIconName(String name) { 
		this.iconName = name; 
	}
	
	public int getColor() { return iconColor; }
	public void setColor(int color) { 
		this.iconColor = color; 
		iconPaint.setColor(color);
	}
	
	private String getIconChar(String name) {
		// Map icon names to Unicode characters
		switch (name.toLowerCase()) {
			case "home": return "\uE88A";
			case "settings": return "\uE8B8";
			case "pie_chart": return "\uE6C4";
			case "bar_chart": return "\uE26B";
			default: return "\uE88A"; // Default to home icon
		}
	}
}
