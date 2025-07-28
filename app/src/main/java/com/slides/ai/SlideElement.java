package com.slides.ai;

import android.content.Context;
import android.graphics.Canvas;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Base class for all slide elements
 */
public abstract class SlideElement {
    protected float x, y, width, height;
    
    public SlideElement(JSONObject json, Context context) throws JSONException {
        x = dpToPx((float) getJsonNumber(json, "x"), context);
        y = dpToPx((float) getJsonNumber(json, "y"), context);
        width = dpToPx((float) getJsonNumber(json, "width"), context);
        height = dpToPx((float) getJsonNumber(json, "height"), context);
    }
    
    private static double getJsonNumber(JSONObject json, String key) throws JSONException {
        Object value = json.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
    
    public abstract void draw(Canvas canvas);
    
    public boolean containsPoint(float px, float py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }
    
    // Helper method for dp to px conversion
    protected static float dpToPx(float dp, Context context) {
        return dp * context.getResources().getDisplayMetrics().density;
    }
    
    // Add abstract toJson method
    public abstract JSONObject toJson() throws JSONException;
}