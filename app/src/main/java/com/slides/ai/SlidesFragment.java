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
import java.io.IOException;
import android.webkit.WebView;


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

public class SlidesFragment extends Fragment implements SlideRenderer.ElementSelectionListener, CustomizationManager.ImageSelectionCallback {

    private WebView slideWebView;
    
    private MaterialButton btnPreviousSlide;
    private MaterialButton btnNextSlide;
    private MaterialButton btnAddSlide;
    private TextView slideCounter;
    
    // Bottom customization toolbar
    private MaterialCardView customizationToolbar;
    private TextView toolbarTitle;
    private MaterialButton btnCloseToolbar;
    private View textOptions;
    private View imageOptions;
    private View shapeOptions;
    private MaterialButton btnMoreOptions;
    
    // Text customization controls
    private TextInputEditText editFontSize;
    private MaterialButton btnTextColor;
    private ChipGroup fontWeightChips;
    private ChipGroup textAlignmentChips;
    
    // Image customization controls
    private Slider sliderCornerRadius;
    private MaterialButton btnReplaceImage;
    
    // Shape customization controls
    private MaterialButton btnShapeFillColor;
    private MaterialButton btnShapeStrokeColor;
    private Slider sliderOpacity;
    private Slider sliderStrokeWidth;
    
    private List<JSONObject> slides = new ArrayList<>();
    private int currentSlideIndex = 0;
    private SlideElement selectedElement = null;
    
    private CustomizationManager customizationManager;
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
        setupCustomizationToolbar();
        updateNavigationControls();

        return view;
    }

    private void initViews(View view) {
        slideWebView = view.findViewById(R.id.slide_webview);
        btnPreviousSlide = view.findViewById(R.id.btn_previous_slide);
        btnNextSlide = view.findViewById(R.id.btn_next_slide);
        btnAddSlide = view.findViewById(R.id.btn_add_slide);
        slideCounter = view.findViewById(R.id.slide_counter);

        // Customization toolbar views
        customizationToolbar = view.findViewById(R.id.customization_toolbar);
        toolbarTitle = view.findViewById(R.id.toolbar_title);
        btnCloseToolbar = view.findViewById(R.id.btn_close_toolbar);
        textOptions = view.findViewById(R.id.text_options);
        imageOptions = view.findViewById(R.id.image_options);
        shapeOptions = view.findViewById(R.id.shape_options);
        btnMoreOptions = view.findViewById(R.id.btn_more_options);

        // Text customization controls
        editFontSize = view.findViewById(R.id.edit_font_size);
        btnTextColor = view.findViewById(R.id.btn_text_color);
        fontWeightChips = view.findViewById(R.id.font_weight_chips);
        textAlignmentChips = view.findViewById(R.id.text_alignment_chips);

        // Image customization controls
        sliderCornerRadius = view.findViewById(R.id.slider_corner_radius);
        btnReplaceImage = view.findViewById(R.id.btn_replace_image);

        // Shape customization controls
        btnShapeFillColor = view.findViewById(R.id.btn_shape_fill_color);
        btnShapeStrokeColor = view.findViewById(R.id.btn_shape_stroke_color);
        sliderOpacity = view.findViewById(R.id.slider_opacity);
        sliderStrokeWidth = view.findViewById(R.id.slider_stroke_width);

        slideWebView.getSettings().setJavaScriptEnabled(true);

        btnPreviousSlide.setOnClickListener(v -> {
            if (slideWebView != null) {
                slideWebView.evaluateJavascript("Reveal.prev();", null);
            }
        });
        btnNextSlide.setOnClickListener(v -> {
            if (slideWebView != null) {
                slideWebView.evaluateJavascript("Reveal.next();", null);
            }
        });
        btnAddSlide.setOnClickListener(v -> {
            if (navigationListener != null) {
                navigationListener.onAddSlideRequested();
            }
        });
    }

    private void setupSlideRenderer() {
        // No setup needed for WebView
    }

    private void setupCustomizationToolbar() {
        btnCloseToolbar.setOnClickListener(v -> hideCustomizationToolbar());

        // Text customization listeners
        setupTextCustomizationListeners();
        
        // Image customization listeners
        setupImageCustomizationListeners();
        
        // Shape customization listeners
        setupShapeCustomizationListeners();

        // More options button
        btnMoreOptions.setOnClickListener(v -> {
            if (selectedElement != null && customizationManager != null) {
                customizationManager.showElementCustomizationDialog(selectedElement);
            }
        });
    }

    private void setupTextCustomizationListeners() {
        // Font size listener
        editFontSize.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (selectedElement instanceof TextElement) {
                    try {
                        float newSize = Float.parseFloat(s.toString());
                        if (newSize > 0 && newSize <= 100) {
                            ((TextElement) selectedElement).fontSize = newSize;
                            ((TextElement) selectedElement).createTextLayout();
                            slideView.invalidate();
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        });

        // Text color listener
        btnTextColor.setOnClickListener(v -> {
            if (selectedElement instanceof TextElement && customizationManager != null) {
                customizationManager.showColorPickerDialog(color -> {
                    ((TextElement) selectedElement).color = color;
                    btnTextColor.setBackgroundTintList(ColorStateList.valueOf(color));
                    ((TextElement) selectedElement).createTextLayout();
                    slideView.invalidate();
                });
            }
        });

        // Font weight listeners
        fontWeightChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (selectedElement instanceof TextElement) {
                TextElement textElement = (TextElement) selectedElement;
                textElement.bold = checkedIds.contains(R.id.chip_bold);
                textElement.createTextLayout();
                slideView.invalidate();
            }
        });

        // Text alignment listeners
        textAlignmentChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (selectedElement instanceof TextElement) {
                TextElement textElement = (TextElement) selectedElement;
                if (checkedIds.contains(R.id.chip_align_left)) {
                    textElement.alignment = "left";
                } else if (checkedIds.contains(R.id.chip_align_center)) {
                    textElement.alignment = "center";
                } else if (checkedIds.contains(R.id.chip_align_right)) {
                    textElement.alignment = "right";
                }
                textElement.createTextLayout();
                slideView.invalidate();
            }
        });
    }

    private void setupImageCustomizationListeners() {
        // Corner radius listener
        sliderCornerRadius.addOnChangeListener((slider, value, fromUser) -> {
            if (selectedElement instanceof ImageElement) {
                ImageElement imageElement = (ImageElement) selectedElement;
                imageElement.cornerRadius = dpToPx(value);
                imageElement.updatePath();
                slideView.invalidate();
            }
        });

        // Replace image listener
        btnReplaceImage.setOnClickListener(v -> {
            if (selectedElement instanceof ImageElement) {
                openImagePicker();
            }
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, 1001);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            if (selectedElement instanceof ImageElement) {
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), imageUri);
                    ((ImageElement) selectedElement).setBitmap(bitmap);
                    slideView.invalidate();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void setupShapeCustomizationListeners() {
        // Fill color listener
        btnShapeFillColor.setOnClickListener(v -> {
            if (selectedElement instanceof ShapeElement && customizationManager != null) {
                customizationManager.showColorPickerDialog(color -> {
                    ((ShapeElement) selectedElement).color = color;
                    btnShapeFillColor.setBackgroundTintList(ColorStateList.valueOf(color));
                    slideView.invalidate();
                });
            }
        });

        // Stroke color listener
        btnShapeStrokeColor.setOnClickListener(v -> {
            if (selectedElement instanceof ShapeElement && customizationManager != null) {
                customizationManager.showColorPickerDialog(color -> {
                    ((ShapeElement) selectedElement).strokeColor = color;
                    btnShapeStrokeColor.setBackgroundTintList(ColorStateList.valueOf(color));
                    slideView.invalidate();
                });
            }
        });

        // Opacity listener
        sliderOpacity.addOnChangeListener((slider, value, fromUser) -> {
            if (selectedElement instanceof ShapeElement) {
                ((ShapeElement) selectedElement).opacity = value / 100f;
                slideView.invalidate();
            }
        });
    }

    @Override
    public void onElementSelected(SlideElement element) {
        // Not used with WebView
    }

    private void showCustomizationToolbar(SlideElement element) {
        // Not used with WebView
    }

    private void setupTextElementUI(TextElement element) {
        // Not used with WebView
    }

    private void setupImageElementUI(ImageElement element) {
        // Not used with WebView
    }

    private void setupShapeElementUI(ShapeElement element) {
        // Not used with WebView
    }

    private void hideCustomizationToolbar() {
        customizationToolbar.setVisibility(View.GONE);
    }

    public void setSlideHtml(String html) {
        if (slideWebView != null) {
            slideWebView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
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
            hideCustomizationToolbar(); // Hide toolbar when switching slides
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
            // Not used with WebView
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
        // Not used with WebView
    }
}
