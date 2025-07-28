package com.slides.ai;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;

public class ChartElement extends SlideElement {
	private String chartType;
	private ArrayList<Float> values;
	private ArrayList<Integer> colors;
	private ArrayList<String> labels;
	private Paint chartPaint;
	private Paint textPaint;
	private boolean showLegend;
	private Context context; // Add this line
	
	public ChartElement(JSONObject json, Context context) throws JSONException {
		super(json, context);
		this.context = context; // Initialize context
		chartType = json.getString("chartType");
		showLegend = json.optBoolean("showLegend", true);
		
		values = new ArrayList<>();
		colors = new ArrayList<>();
		labels = new ArrayList<>();
		
		JSONArray data = json.getJSONArray("data");
		for (int i = 0; i < data.length(); i++) {
			JSONObject item = data.getJSONObject(i);
			values.add((float) item.getDouble("value"));
			colors.add(Color.parseColor(item.getString("color")));
			labels.add(item.getString("label"));
		}
		
		initPaints();
	}
	
	private void initPaints() {
		chartPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		chartPaint.setStyle(Paint.Style.FILL);
		
		textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		textPaint.setColor(Color.BLACK);
		textPaint.setTextSize(dpToPx(10)); // Remove context parameter
	}
	
	// Add this helper method
	private int dpToPx(float dp) {
		return (int) (dp * context.getResources().getDisplayMetrics().density);
	}
	
	@Override
	public void draw(Canvas canvas, float canvasWidth, float canvasHeight) {
		float xPx = getXPx(canvasWidth);
		float yPx = getYPx(canvasHeight);
		float wPx = getWidthPx(canvasWidth);
		float hPx = getHeightPx(canvasHeight);
		canvas.save();
		canvas.translate(xPx, yPx);
		switch (chartType) {
			case "bar":
				drawBarChart(canvas, wPx, hPx);
				break;
			case "pie":
				drawPieChart(canvas, wPx, hPx);
				break;
		}
		if (showLegend) {
			drawLegend(canvas, wPx, hPx);
		}
		canvas.restore();
	}
	private void drawBarChart(Canvas canvas, float wPx, float hPx) {
		float barWidth = wPx / values.size() * 0.8f;
		float gap = wPx / values.size() * 0.2f;
		float maxValue = getMaxValue();
		for (int i = 0; i < values.size(); i++) {
			float left = i * (barWidth + gap);
			float top = hPx - (values.get(i) / maxValue * hPx);
			float right = left + barWidth;
			float bottom = hPx;
			chartPaint.setColor(colors.get(i));
			canvas.drawRect(left, top, right, bottom, chartPaint);
		}
	}
	private void drawPieChart(Canvas canvas, float wPx, float hPx) {
		RectF rect = new RectF(0, 0, Math.min(wPx, hPx), Math.min(wPx, hPx));
		float total = getTotalValue();
		float startAngle = 0;
		for (int i = 0; i < values.size(); i++) {
			float sweepAngle = (values.get(i) / total) * 360;
			chartPaint.setColor(colors.get(i));
			canvas.drawArc(rect, startAngle, sweepAngle, true, chartPaint);
			startAngle += sweepAngle;
		}
	}
	private void drawLegend(Canvas canvas, float wPx, float hPx) {
		float legendX = wPx + dpToPx(10);
		float legendY = dpToPx(20);
		float boxSize = dpToPx(12);
		for (int i = 0; i < labels.size(); i++) {
			chartPaint.setColor(colors.get(i));
			canvas.drawRect(legendX, legendY, legendX + boxSize, legendY + boxSize, chartPaint);
			canvas.drawText(labels.get(i), legendX + boxSize + dpToPx(5), legendY + boxSize, textPaint);
			legendY += boxSize + dpToPx(8);
		}
	}
	
	public String getChartType() { return chartType; }
	public void setChartType(String type) { 
		this.chartType = type; 
	}
	
	public JSONArray getData() {
		JSONArray arr = new JSONArray();
		for (int i = 0; i < values.size(); i++) {
			JSONObject item = new JSONObject();
			try {
				item.put("value", values.get(i));
				item.put("label", labels.get(i));
				item.put("color", String.format("#%06X", (0xFFFFFF & colors.get(i))));
			} catch (JSONException e) { /* handle */ }
			arr.put(item);
		}
		return arr;
	}
	
	@Override
	public JSONObject toJson() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("type", "chart");
		json.put("x", x);
		json.put("y", y);
		json.put("width", width);
		json.put("height", height);
		json.put("chartType", chartType);
		json.put("showLegend", showLegend);
		JSONArray dataArray = new JSONArray();
		for (int i = 0; i < values.size(); i++) {
			JSONObject item = new JSONObject();
			item.put("value", values.get(i));
			item.put("label", labels.get(i));
			item.put("color", String.format("#%06X", (0xFFFFFF & colors.get(i))));
			dataArray.put(item);
		}
		json.put("data", dataArray);
		return json;
	}
	
	public void setData(JSONArray data) {
		// Parse and update chart data
	}
	
	private float getMaxValue() {
		float max = 0;
		for (float val : values) {
			if (val > max) max = val;
		}
		return max;
	}
	
	private float getTotalValue() {
		float total = 0;
		for (float val : values) {
			total += val;
		}
		return total;
	}
}
