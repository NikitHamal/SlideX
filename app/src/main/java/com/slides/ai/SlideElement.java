package com.slides.ai;

import android.content.Context;
import android.graphics.Canvas;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Base class for all slide elements
 */
public abstract class SlideElement {
    protected float x, y, width, height; // Now percentages (0.0–1.0)
    
    public SlideElement(JSONObject json, Context context) throws JSONException {
        // Expect x, y, width, height as percentages in JSON
        x = (float) json.optDouble("x", 0.0);
        y = (float) json.optDouble("y", 0.0);
        width = (float) json.optDouble("width", 0.2); // default 20%
        height = (float) json.optDouble("height", 0.1); // default 10%
    }
    
    // Convert percentage to pixel value
    public float getXPx(float canvasWidth) { return x * canvasWidth; }
    public float getYPx(float canvasHeight) { return y * canvasHeight; }
    public float getWidthPx(float canvasWidth) { return width * canvasWidth; }
    public float getHeightPx(float canvasHeight) { return height * canvasHeight; }
    
    public abstract void draw(Canvas canvas, float canvasWidth, float canvasHeight);
    
    public boolean containsPoint(float px, float py, float canvasWidth, float canvasHeight) {
        float xPx = getXPx(canvasWidth);
        float yPx = getYPx(canvasHeight);
        float wPx = getWidthPx(canvasWidth);
        float hPx = getHeightPx(canvasHeight);
        return px >= xPx && px <= xPx + wPx && py >= yPx && py <= yPx + hPx;
    }
    
    // Add abstract toJson method
    public abstract JSONObject toJson() throws JSONException;
}