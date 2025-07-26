package com.slides.ai;

import android.content.Context;
import android.graphics.Canvas;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Base class for all slide elements
 */
public abstract class SlideElement {
    protected int x, y, width, height;
    
    public SlideElement(JSONObject json, Context context) throws JSONException {
        x = dpToPx(json.getInt("x"), context);
        y = dpToPx(json.getInt("y"), context);
        width = dpToPx(json.getInt("width"), context);
        height = dpToPx(json.getInt("height"), context);
    }
    
    public abstract void draw(Canvas canvas);
    
    public boolean containsPoint(float px, float py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }
    
    // Helper method for dp to px conversion
    protected static int dpToPx(float dp, Context context) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }
    
    // Add abstract toJson method
    public abstract JSONObject toJson() throws JSONException;
}