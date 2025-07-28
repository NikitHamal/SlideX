package com.slides.ai;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SlidesFragment extends Fragment {

    private MaterialCardView slide;
    private CustomView slideView;
    private SlideRenderer slideRenderer;
    private HashMap<String, Bitmap> imageCache = new HashMap<>();
    
    private MaterialButton btnPreviousSlide;
    private MaterialButton btnNextSlide;
    private MaterialButton btnAddSlide;
    private TextView slideCounter;
    
    private List<JSONObject> slides = new ArrayList<>();
    private int currentSlideIndex = 0;
    
    private SlideNavigationListener navigationListener;

    public interface SlideNavigationListener {
        void onAddSlideRequested();
        void onSlideChanged(int slideIndex);
    }

    public void setSlideNavigationListener(SlideNavigationListener listener) {
        this.navigationListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_slides, container, false);

        initViews(view);
        setupSlideRenderer();
        updateNavigationControls();

        return view;
    }

    private void initViews(View view) {
        slide = view.findViewById(R.id.slide);
        btnPreviousSlide = view.findViewById(R.id.btn_previous_slide);
        btnNextSlide = view.findViewById(R.id.btn_next_slide);
        btnAddSlide = view.findViewById(R.id.btn_add_slide);
        slideCounter = view.findViewById(R.id.slide_counter);

        slideView = new CustomView(getContext());
        slide.addView(slideView);

        btnPreviousSlide.setOnClickListener(v -> navigateToPreviousSlide());
        btnNextSlide.setOnClickListener(v -> navigateToNextSlide());
        btnAddSlide.setOnClickListener(v -> {
            if (navigationListener != null) {
                navigationListener.onAddSlideRequested();
            }
        });
    }

    private void setupSlideRenderer() {
        slideRenderer = new SlideRenderer(getContext(), slideView, imageCache);
        // slideRenderer.setElementSelectionListener(this); // TODO: Implement this
    }

    public void setSlideData(JSONObject json) {
        if (slideRenderer != null) {
            slideRenderer.setSlideData(json);
            slideView.invalidate();
        }
    }

    public void setSlides(List<JSONObject> slideList) {
        this.slides = new ArrayList<>(slideList);
        if (currentSlideIndex >= slides.size()) {
            currentSlideIndex = Math.max(0, slides.size() - 1);
        }
        loadCurrentSlide();
        updateNavigationControls();
    }

    public void addSlide(JSONObject slideData) {
        slides.add(slideData);
        updateNavigationControls();
    }

    public void navigateToSlide(int index) {
        if (index >= 0 && index < slides.size()) {
            currentSlideIndex = index;
            loadCurrentSlide();
            updateNavigationControls();
            if (navigationListener != null) {
                navigationListener.onSlideChanged(index);
            }
        }
    }

    private void navigateToPreviousSlide() {
        if (currentSlideIndex > 0) {
            navigateToSlide(currentSlideIndex - 1);
        }
    }

    private void navigateToNextSlide() {
        if (currentSlideIndex < slides.size() - 1) {
            navigateToSlide(currentSlideIndex + 1);
        }
    }

    private void loadCurrentSlide() {
        if (currentSlideIndex < slides.size()) {
            setSlideData(slides.get(currentSlideIndex));
        }
    }

    private void updateNavigationControls() {
        int slideCount = slides.size();
        
        // Update counter
        slideCounter.setText((currentSlideIndex + 1) + "/" + slideCount);
        
        // Show/hide navigation buttons based on slide count and position
        boolean showNavigation = slideCount > 1;
        btnPreviousSlide.setVisibility(showNavigation && currentSlideIndex > 0 ? View.VISIBLE : View.GONE);
        btnNextSlide.setVisibility(showNavigation && currentSlideIndex < slideCount - 1 ? View.VISIBLE : View.GONE);
    }

    public SlideRenderer getSlideRenderer() {
        return slideRenderer;
    }

    public int getCurrentSlideIndex() {
        return currentSlideIndex;
    }

    public int getSlideCount() {
        return slides.size();
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
