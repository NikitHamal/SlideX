package com.slides.ai;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.request.transition.Transition;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
* Image element with support for rounded corners
*/
public class ImageElement extends SlideElement {
	String url;
	float cornerRadius;
	private Path clipPath;
	private Paint paint;
	private Context context;
	private String customImageKey; // For custom images selected by user
	
	public ImageElement(JSONObject json, Context context) throws JSONException {
		super(json, context);
		this.context = context;
		url = json.getString("url");
		cornerRadius = json.optInt("cornerRadius", 0);
		
		paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(Color.LTGRAY); // Default color for placeholder
		
		updatePath();
	}
	
	public void updatePath() {
		clipPath = new Path();
		RectF rect = new RectF(0, 0, width, height);
		clipPath.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW);
	}
	
	public void setCustomImage(String imageKey) {
		this.customImageKey = imageKey;
	}
	
	@Override
	public JSONObject toJson() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("type", "image");
		json.put("x", x);
		json.put("y", y);
		json.put("width", width);
		json.put("height", height);
		json.put("url", url);
		json.put("cornerRadius", cornerRadius);
		if (customImageKey != null) {
			json.put("customImageKey", customImageKey);
		}
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
		if (cornerRadius > 0) {
			clipPath = new Path();
			RectF rect = new RectF(0, 0, wPx, hPx);
			clipPath.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW);
			canvas.clipPath(clipPath);
		}
		canvas.drawRect(0, 0, wPx, hPx, paint);
		String imageUrl = (customImageKey != null) ? customImageKey : url;
		// Glide async loading is not ideal for draw, but keep for now
		Glide.with(context)
			.asBitmap()
			.load(imageUrl)
			.into(new CustomTarget<Bitmap>() {
				@Override
				public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {
					float scale = Math.min(wPx / bitmap.getWidth(), hPx / bitmap.getHeight());
					int scaledWidth = (int) (bitmap.getWidth() * scale);
					int scaledHeight = (int) (bitmap.getHeight() * scale);
					int left = (int) ((wPx - scaledWidth) / 2);
					int top = (int) ((hPx - scaledHeight) / 2);
					RectF destRect = new RectF(left, top, left + scaledWidth, top + scaledHeight);
					canvas.drawBitmap(bitmap, null, destRect, null);
				}
				@Override
				public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) {}
			});
		canvas.restore();
	}
}
