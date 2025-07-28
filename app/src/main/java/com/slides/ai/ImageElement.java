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
import android.util.Log;
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

	public void setBitmap(Bitmap bitmap) {
		// This is a placeholder.
		// In a real app, you would likely want to save the bitmap to a file and store the URI.
		// For now, we'll just store it in the cache.
		String imageKey = "custom_image_" + System.currentTimeMillis();
		this.customImageKey = imageKey;
		// A more complete implementation would handle the image cache in the NetworkManager
		// or a dedicated ImageCache class.
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
	public void draw(Canvas canvas) {
		Log.d("ImageElement", "Drawing image element: " + url + " at (" + x + ", " + y + ") with width " + width + " and height " + height);
		canvas.save();
		canvas.translate(x, y);
		canvas.rotate(rotation, width / 2, height / 2);

		// Clip to rounded rectangle if corner radius > 0
		if (cornerRadius > 0) {
			canvas.clipPath(clipPath);
		}

		// Draw placeholder background
		canvas.drawRect(0, 0, width, height, paint);

        String imageUrl = (customImageKey != null) ? customImageKey : url;

		Glide.with(context)
			.asBitmap()
			.load(imageUrl)
			.into(new CustomTarget<Bitmap>() {
				@Override
				public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {
					// Scale bitmap to fit element dimensions while maintaining aspect ratio
					float scale = Math.min((float) width / bitmap.getWidth(), (float) height / bitmap.getHeight());
					int scaledWidth = (int) (bitmap.getWidth() * scale);
					int scaledHeight = (int) (bitmap.getHeight() * scale);

					// Center the image
					int left = (width - scaledWidth) / 2;
					int top = (height - scaledHeight) / 2;

					RectF destRect = new RectF(left, top, left + scaledWidth, top + scaledHeight);
					canvas.drawBitmap(bitmap, null, destRect, null);
				}

				@Override
				public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) {
					// Do nothing
				}
			});

		canvas.restore();
	}
}
