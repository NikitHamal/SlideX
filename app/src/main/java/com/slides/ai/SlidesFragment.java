package com.slides.ai;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;

import org.json.JSONObject;

import java.util.HashMap;

public class SlidesFragment extends Fragment {

    private MaterialCardView slide;
    private CustomView slideView;
    private SlideRenderer slideRenderer;
    private HashMap<String, Bitmap> imageCache = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_slides, container, false);

        slide = view.findViewById(R.id.slide);
        slideView = new CustomView(getContext());
        slide.addView(slideView);

        slideRenderer = new SlideRenderer(getContext(), slideView, imageCache);
        // slideRenderer.setElementSelectionListener(this); // TODO: Implement this

        return view;
    }

    public void setSlideData(JSONObject json) {
        if (slideRenderer != null) {
            slideRenderer.setSlideData(json);
            slideView.invalidate();
        }
    }

    public SlideRenderer getSlideRenderer() {
        return slideRenderer;
    }

    private class CustomView extends View {
        public CustomView(Context context) {
            super(context);
            setClickable(true);
            setFocusable(true);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (slideRenderer != null) {
                slideRenderer.draw(canvas);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            return slideRenderer != null && slideRenderer.handleTouchEvent(event);
        }
    }
}
