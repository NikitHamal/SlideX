package com.slides.ai;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import org.json.JSONException;
import org.json.JSONObject;

/**
* Shape element with support for various shape types and styling
*/
public class ShapeElement extends SlideElement {
	String shapeType;
	int color;
	float cornerRadius;
	float opacity;
	float strokeWidth;
	int strokeColor;
	private Path shapePath;
	private Paint fillPaint;
	private Paint strokePaint;
	private Context context;
	
	public ShapeElement(JSONObject json, Context context) throws JSONException {
		super(json, context);
		this.context = context;
		shapeType = json.optString("shapeType", "rectangle");
		color = Color.parseColor(json.optString("color", "#2196F3"));
		cornerRadius = json.optInt("cornerRadius", 0);
		opacity = (float) json.optDouble("opacity", 1.0);
		strokeWidth = json.optInt("strokeWidth", 0);
		strokeColor = Color.parseColor(json.optString("strokeColor", "#000000"));
		
		// Initialize paints
		fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		fillPaint.setStyle(Paint.Style.FILL);
		fillPaint.setColor(color);
		fillPaint.setAlpha((int) (opacity * 255));
		
		strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		strokePaint.setStyle(Paint.Style.STROKE);
		strokePaint.setColor(strokeColor);
		strokePaint.setStrokeWidth(strokeWidth);
		
		createShapePath();
	}
	
	public void createShapePath() {
		shapePath = new Path();
		
		switch (shapeType.toLowerCase()) {
			case "rectangle":
			if (cornerRadius > 0) {
				RectF rect = new RectF(0, 0, width, height);
				shapePath.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW);
			} else {
				shapePath.addRect(0, 0, width, height, Path.Direction.CW);
			}
			break;
			
			case "oval":
			RectF ovalRect = new RectF(0, 0, width, height);
			shapePath.addOval(ovalRect, Path.Direction.CW);
			break;
			
			case "line":
			shapePath.moveTo(0, 0);
			shapePath.lineTo(width, height);
			break;
			
			case "triangle":
			shapePath.moveTo(width / 2, 0);
			shapePath.lineTo(width, height);
			shapePath.lineTo(0, height);
			shapePath.close();
			break;
			
			case "star":
			createStarPath();
			break;
			
			case "hexagon":
			createHexagonPath();
			break;
			
			default:
			// Default to rectangle
			shapePath.addRect(0, 0, width, height, Path.Direction.CW);
			break;
		}
	}
	
	private void createStarPath() {
		float centerX = width / 2;
		float centerY = height / 2;
		float outerRadius = Math.min(width, height) / 2;
		float innerRadius = outerRadius * 0.4f;
		int numPoints = 5;
		
		shapePath.moveTo(centerX, centerY - outerRadius);
		
		for (int i = 1; i < numPoints * 2; i++) {
			float radius = (i % 2 == 0) ? outerRadius : innerRadius;
			float angle = (float) (Math.PI * i / numPoints);
			float x = (float) (centerX + radius * Math.sin(angle));
			float y = (float) (centerY - radius * Math.cos(angle));
			shapePath.lineTo(x, y);
		}
		
		shapePath.close();
	}
	
	private void createHexagonPath() {
		float centerX = width / 2;
		float centerY = height / 2;
		float radius = Math.min(width, height) / 2;
		
		shapePath.moveTo(centerX + radius, centerY);
		
		for (int i = 1; i < 6; i++) {
			float angle = (float) (Math.PI / 3 * i);
			float x = (float) (centerX + radius * Math.cos(angle));
			float y = (float) (centerY + radius * Math.sin(angle));
			shapePath.lineTo(x, y);
		}
		
		shapePath.close();
	}
	
	@Override
	public JSONObject toJson() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("type", "shape");
		json.put("x", x);
		json.put("y", y);
		json.put("width", width);
		json.put("height", height);
		json.put("shapeType", shapeType);
		json.put("color", String.format("#%06X", (0xFFFFFF & color)));
		json.put("cornerRadius", cornerRadius);
		json.put("opacity", opacity);
		json.put("strokeWidth", strokeWidth);
		json.put("strokeColor", String.format("#%06X", (0xFFFFFF & strokeColor)));
		return json;
	}
	
	@Override
	public void draw(Canvas canvas, float canvasWidth, float canvasHeight) {
		float xPx = getXPx(canvasWidth);
		float yPx = getYPx(canvasHeight);
		float wPx = getWidthPx(canvasWidth);
		float hPx = getHeightPx(canvasHeight);
		canvas.save();
		canvas.translate(xPx, yPx);
		// Recreate shape path if needed
		createShapePath(wPx, hPx);
		canvas.drawPath(shapePath, fillPaint);
		if (strokeWidth > 0) {
			canvas.drawPath(shapePath, strokePaint);
		}
		canvas.restore();
	}

	public void createShapePath(float wPx, float hPx) {
		shapePath = new Path();
		switch (shapeType.toLowerCase()) {
			case "rectangle":
				if (cornerRadius > 0) {
					RectF rect = new RectF(0, 0, wPx, hPx);
					shapePath.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW);
				} else {
					shapePath.addRect(0, 0, wPx, hPx, Path.Direction.CW);
				}
				break;
			case "oval":
				RectF ovalRect = new RectF(0, 0, wPx, hPx);
				shapePath.addOval(ovalRect, Path.Direction.CW);
				break;
			case "line":
				shapePath.moveTo(0, 0);
				shapePath.lineTo(wPx, hPx);
				break;
			case "triangle":
				shapePath.moveTo(wPx / 2, 0);
				shapePath.lineTo(wPx, hPx);
				shapePath.lineTo(0, hPx);
				shapePath.close();
				break;
			case "star":
				// ... implement star with wPx, hPx ...
				break;
			case "hexagon":
				// ... implement hexagon with wPx, hPx ...
				break;
			default:
				shapePath.addRect(0, 0, wPx, hPx, Path.Direction.CW);
				break;
		}
	}
}
