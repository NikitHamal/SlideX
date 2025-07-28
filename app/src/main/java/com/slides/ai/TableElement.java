package com.slides.ai;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
* Table element with support for headers, cells, and borders
*/
public class TableElement extends SlideElement {
	int rows;
	int columns;
	String[][] data;
	int headerColor;
	int cellColor;
	int borderColor;
	float borderWidth;
	private Paint headerPaint;
	private Paint cellPaint;
	private Paint borderPaint;
	private Paint textPaint;
	private Context context;
	
	public TableElement(JSONObject json, Context context) throws JSONException {
		super(json, context);
		this.context = context;
		rows = json.optInt("rows", 3);
		columns = json.optInt("columns", 3);
		headerColor = Color.parseColor(json.optString("headerColor", "#E3F2FD"));
		cellColor = Color.parseColor(json.optString("cellColor", "#FFFFFF"));
		borderColor = Color.parseColor(json.optString("borderColor", "#2196F3"));
		borderWidth = json.optInt("borderWidth", 1);
		
		// Parse data
		data = new String[rows][columns];
		if (json.has("data")) {
			JSONArray jsonData = json.getJSONArray("data");
			for (int i = 0; i < Math.min(rows, jsonData.length()); i++) {
				JSONArray rowData = jsonData.getJSONArray(i);
				for (int j = 0; j < Math.min(columns, rowData.length()); j++) {
					data[i][j] = rowData.getString(j);
				}
			}
		} else {
			// Create default data
			for (int i = 0; i < rows; i++) {
				for (int j = 0; j < columns; j++) {
					data[i][j] = "Cell " + i + "," + j;
				}
			}
		}
		
		initializePaints();
	}
	
	public void initializePaints() {
		headerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		headerPaint.setStyle(Paint.Style.FILL);
		headerPaint.setColor(headerColor);
		
		cellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		cellPaint.setStyle(Paint.Style.FILL);
		cellPaint.setColor(cellColor);
		
		borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		borderPaint.setStyle(Paint.Style.STROKE);
		borderPaint.setColor(borderColor);
		borderPaint.setStrokeWidth(borderWidth);
		
		textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		textPaint.setColor(Color.BLACK);
		textPaint.setTextSize(dpToPx(12, context));
		
		// Try to load medium font for headers
		try {
			Typeface mediumFont = Typeface.createFromAsset(context.getAssets(), "med.ttf");
			textPaint.setTypeface(mediumFont);
		} catch (Exception e) {
			// Fallback to system font
			textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
		}
	}
	
	@Override
	public JSONObject toJson() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("type", "table");
		json.put("x", x);
		json.put("y", y);
		json.put("width", width);
		json.put("height", height);
		json.put("rows", rows);
		json.put("columns", columns);
		json.put("headerColor", String.format("#%06X", (0xFFFFFF & headerColor)));
		json.put("cellColor", String.format("#%06X", (0xFFFFFF & cellColor)));
		json.put("borderColor", String.format("#%06X", (0xFFFFFF & borderColor)));
		json.put("borderWidth", borderWidth);
		
		JSONArray dataArray = new JSONArray();
		for (String[] row : data) {
			JSONArray rowArray = new JSONArray();
			for (String cell : row) {
				rowArray.put(cell);
			}
			dataArray.put(rowArray);
		}
		json.put("data", dataArray);
		
		return json;
	}
	
	@Override
	public void draw(Canvas canvas) {
		canvas.save();
		canvas.translate(x, y);
		
		float cellWidth = width / columns;
		float cellHeight = height / rows;
		
		// Draw cells
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < columns; j++) {
				float left = j * cellWidth;
				float top = i * cellHeight;
				float right = left + cellWidth;
				float bottom = top + cellHeight;
				
				// Draw cell background
				Paint bgPaint = (i == 0) ? headerPaint : cellPaint;
				canvas.drawRect(left, top, right, bottom, bgPaint);
				
				// Draw cell border
				if (borderWidth > 0) {
					canvas.drawRect(left, top, right, bottom, borderPaint);
				}
				
				// Draw cell text
				if (data[i][j] != null) {
					String text = data[i][j];
					Rect textBounds = new Rect();
					textPaint.getTextBounds(text, 0, text.length(), textBounds);
					
					// Center text in cell
					float textX = left + (cellWidth - textBounds.width()) / 2f;
					float textY = top + (cellHeight + textBounds.height()) / 2f;
					
					canvas.drawText(text, textX, textY, textPaint);
				}
			}
		}
		
		canvas.restore();
	}
}
