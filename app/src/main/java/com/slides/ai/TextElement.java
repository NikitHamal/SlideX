package com.slides.ai;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import org.json.JSONException;
import org.json.JSONObject;

/**
* Text element with proper text wrapping and alignment
*/
public class TextElement extends SlideElement {
	String content;
	float fontSize;
	int color;
	boolean bold;
	boolean medium;
	boolean italic;
	String alignment;
	private StaticLayout textLayout;
	private TextPaint textPaint;
	private Context context;
	
	public TextElement(JSONObject json, Context context) throws JSONException {
		super(json, context);
		this.context = context;
		content = json.getString("text");
		fontSize = json.optInt("fontSize", 14);
		color = Color.parseColor(json.optString("color", "#000000"));
		bold = json.optBoolean("bold", false);
		medium = false; // New property for medium font
		italic = json.optBoolean("italic", false);
		alignment = json.optString("alignment", "left");
		
		textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
		
		// Create text layout for proper wrapping
		createTextLayout();
	}
	
	public void createTextLayout() {
		textPaint.setTextSize(spToPx(fontSize, context));
		textPaint.setColor(color);
		
		// Get fonts from MainActivity
		Typeface regularFont = null;
		Typeface mediumFont = null;
		Typeface semiBoldFont = null;
		
		try {
			regularFont = Typeface.createFromAsset(context.getAssets(), "reg.ttf");
			mediumFont = Typeface.createFromAsset(context.getAssets(), "med.ttf");
			semiBoldFont = Typeface.createFromAsset(context.getAssets(), "sem.ttf");
		} catch (Exception e) {
			// Fallback to system fonts
			regularFont = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL);
			mediumFont = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL);
			semiBoldFont = Typeface.create(Typeface.DEFAULT, Typeface.BOLD);
		}
		
		// Set typeface based on style using custom fonts
		if (bold) {
			textPaint.setTypeface(semiBoldFont);
		} else if (medium) {
			textPaint.setTypeface(mediumFont);
		} else {
			textPaint.setTypeface(regularFont);
		}
		
		// Add italic if needed
		if (italic) {
			Typeface currentTypeface = textPaint.getTypeface();
			textPaint.setTypeface(Typeface.create(currentTypeface, Typeface.ITALIC));
		}
		
		// Set text alignment
		Layout.Alignment textAlignment = Layout.Alignment.ALIGN_NORMAL; // Left
		if (alignment.equalsIgnoreCase("center")) {
			textAlignment = Layout.Alignment.ALIGN_CENTER;
		} else if (alignment.equalsIgnoreCase("right")) {
			textAlignment = Layout.Alignment.ALIGN_OPPOSITE;
		}
		
		// Create static layout for text wrapping
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
			textLayout = StaticLayout.Builder.obtain(content, 0, content.length(), textPaint, width)
			.setAlignment(textAlignment)
			.setLineSpacing(0, 1.0f)
			.setIncludePad(false)
			.build();
		} else {
			// For older Android versions
			textLayout = new StaticLayout(content, textPaint, width, textAlignment, 1.0f, 0, false);
		}
	}
	
	@Override
	public JSONObject toJson() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("type", "text");
		json.put("x", x);
		json.put("y", y);
		json.put("width", width);
		json.put("height", height);
		json.put("text", content);
		json.put("fontSize", fontSize);
		json.put("color", String.format("#%06X", (0xFFFFFF & color)));
		json.put("bold", bold);
		json.put("medium", medium);
		json.put("italic", italic);
		json.put("alignment", alignment);
		return json;
	}
	
	@Override
	public void draw(Canvas canvas) {
		canvas.save();
		canvas.translate(x, y);
		textLayout.draw(canvas);
		canvas.restore();
	}
	
	private float spToPx(float sp, Context context) {
		return sp * context.getResources().getDisplayMetrics().scaledDensity;
	}
}
