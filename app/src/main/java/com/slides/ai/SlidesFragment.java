package com.slides.ai;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.webkit.WebView;
import java.io.IOException;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SlidesFragment extends Fragment implements CustomizationManager.ImageSelectionCallback {

    private MaterialCardView slide;
    private WebView slideWebView;
    private RevealJsRenderer revealJsRenderer;
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
        View view = inflater.inflate(R.layout.fragment_slides_revealjs, container, false);

        initViews(view);
        setupRevealJsRenderer();
        updateNavigationControls();

        return view;
    }

    private void initViews(View view) {
        slide = view.findViewById(R.id.slide);
        slideWebView = view.findViewById(R.id.slide_webview);
        btnPreviousSlide = view.findViewById(R.id.btn_previous_slide);
        btnNextSlide = view.findViewById(R.id.btn_next_slide);
        btnAddSlide = view.findViewById(R.id.btn_add_slide);
        slideCounter = view.findViewById(R.id.slide_counter);

        btnPreviousSlide.setOnClickListener(v -> navigateToPreviousSlide());
        btnNextSlide.setOnClickListener(v -> navigateToNextSlide());
        btnAddSlide.setOnClickListener(v -> {
            if (navigationListener != null) {
                navigationListener.onAddSlideRequested();
            }
        });
    }

    private void setupRevealJsRenderer() {
        revealJsRenderer = new RevealJsRenderer(getContext(), slideWebView, imageCache);

        revealJsRenderer.setSlideChangeListener(new RevealJsRenderer.SlideChangeListener() {
            @Override
            public void onSlideChanged(int slideIndex) {
                currentSlideIndex = slideIndex;
                updateNavigationControls();
                if (navigationListener != null) {
                    navigationListener.onSlideChanged(slideIndex);
                }
            }
        });

        revealJsRenderer.setRevealReadyListener(new RevealJsRenderer.RevealReadyListener() {
            @Override
            public void onRevealReady() {
                // Reveal.js is ready, navigate to current slide
                if (currentSlideIndex > 0) {
                    revealJsRenderer.navigateToSlide(currentSlideIndex);
                }
            }
        });
    }


    public void setSlideData(JSONObject json) {
        if (revealJsRenderer != null) {
            revealJsRenderer.setSlide(json);
        }
    }

    public void setSlides(List<JSONObject> slideList) {
        this.slides = new ArrayList<>(slideList);
        if (currentSlideIndex >= slides.size()) {
            currentSlideIndex = Math.max(0, slides.size() - 1);
        }
        if (revealJsRenderer != null) {
            revealJsRenderer.setSlides(slides);
        }
        updateNavigationControls();
    }

    public void addSlide(JSONObject slideData) {
        slides.add(slideData);
        if (revealJsRenderer != null) {
            revealJsRenderer.addSlide(slideData);
        }
        updateNavigationControls();
    }

    public void navigateToSlide(int index) {
        if (index >= 0 && index < slides.size()) {
            currentSlideIndex = index;
            if (revealJsRenderer != null) {
                revealJsRenderer.navigateToSlide(index);
            }
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

    private void updateNavigationControls() {
        int slideCount = slides.size();

        // Update counter
        slideCounter.setText((currentSlideIndex + 1) + "/" + slideCount);

        // Show/hide navigation buttons based on slide count and position
        boolean showNavigation = slideCount > 1;
        btnPreviousSlide.setVisibility(showNavigation && currentSlideIndex > 0 ? View.VISIBLE : View.GONE);
        btnNextSlide.setVisibility(showNavigation && currentSlideIndex < slideCount - 1 ? View.VISIBLE : View.GONE);
    }

    public RevealJsRenderer getRevealJsRenderer() {
        return revealJsRenderer;
    }

    // Legacy support for code that expects SlideRenderer
    @Deprecated
    public SlideRenderer getSlideRenderer() {
        // Return null since we're not using SlideRenderer anymore
        // Code should be updated to use getRevealJsRenderer() instead
        return null;
    }

    public int getCurrentSlideIndex() {
        return currentSlideIndex;
    }

    public int getSlideCount() {
        return slides.size();
    }

    private int dpToPx(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onImageSelectionRequested(SlideElement element) {
        // Handle image selection - for now, we'll show a placeholder
        // You can implement image picker here in the future
        Toast.makeText(getContext(), "Image selection feature coming soon", Toast.LENGTH_SHORT).show();
    }
}
