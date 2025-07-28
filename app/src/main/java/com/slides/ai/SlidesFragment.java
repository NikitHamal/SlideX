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

    private MaterialCardView slide;
    private CustomView slideView;
    private SlideRenderer slideRenderer;
    private HashMap<String, Bitmap> imageCache = new HashMap<>();
    
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
        slide = view.findViewById(R.id.slide);
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
        slideRenderer.setElementSelectionListener(this);
        
        // Initialize customization manager
        customizationManager = new CustomizationManager(getContext(), slideRenderer);
        customizationManager.setImageSelectionCallback(this);
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
                onImageSelectionRequested(selectedElement);
            }
        });
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
        selectedElement = element;
        showCustomizationToolbar(element);
    }

    private void showCustomizationToolbar(SlideElement element) {
        if (element == null) {
            hideCustomizationToolbar();
            return;
        }

        // Hide all option groups first
        textOptions.setVisibility(View.GONE);
        imageOptions.setVisibility(View.GONE);
        shapeOptions.setVisibility(View.GONE);

        // Update toolbar title and show appropriate options
        if (element instanceof TextElement) {
            toolbarTitle.setText("Text Options");
            textOptions.setVisibility(View.VISIBLE);
            setupTextElementUI((TextElement) element);
        } else if (element instanceof ImageElement) {
            toolbarTitle.setText("Image Options");
            imageOptions.setVisibility(View.VISIBLE);
            setupImageElementUI((ImageElement) element);
        } else if (element instanceof ShapeElement) {
            toolbarTitle.setText("Shape Options");
            shapeOptions.setVisibility(View.VISIBLE);
            setupShapeElementUI((ShapeElement) element);
        } else {
            toolbarTitle.setText("Element Options");
        }

        customizationToolbar.setVisibility(View.VISIBLE);
        setupPositionSizeSliders(selectedElement);
        setupOpacitySlider(selectedElement);
        setupZOrderControls(selectedElement);
    }

    private void setupTextElementUI(TextElement element) {
        // Set font size
        editFontSize.setText(String.valueOf((int) element.fontSize));

        // Set text color
        btnTextColor.setBackgroundTintList(ColorStateList.valueOf(element.color));

        // Set font weight
        if (element.bold) {
            fontWeightChips.check(R.id.chip_bold);
        } else {
            fontWeightChips.check(R.id.chip_regular);
        }

        // Set text alignment
        switch (element.alignment.toLowerCase()) {
            case "center":
                textAlignmentChips.check(R.id.chip_align_center);
                break;
            case "right":
                textAlignmentChips.check(R.id.chip_align_right);
                break;
            default:
                textAlignmentChips.check(R.id.chip_align_left);
                break;
        }
    }

    private void setupImageElementUI(ImageElement element) {
        // Set corner radius - ensure value is compatible with stepSize
        float cornerRadiusValue = element.cornerRadius / dpToPx(1);
        // Round to nearest integer to match stepSize of 1
        sliderCornerRadius.setValue(Math.round(cornerRadiusValue));
    }

    private void setupShapeElementUI(ShapeElement element) {
        // Set fill color
        btnShapeFillColor.setBackgroundTintList(ColorStateList.valueOf(element.color));
        
        // Set stroke color
        btnShapeStrokeColor.setBackgroundTintList(ColorStateList.valueOf(element.strokeColor));
        
        // Set corner radius - ensure value is compatible with stepSize
        float cornerRadiusValue = element.cornerRadius / dpToPx(1);
        sliderCornerRadius.setValue(Math.round(cornerRadiusValue));
        
        // Set stroke width - ensure value is compatible with stepSize  
        float strokeWidthValue = element.strokeWidth / dpToPx(1);
        sliderStrokeWidth.setValue(Math.round(strokeWidthValue));
        
        // Set opacity - ensure value is compatible with stepSize (0-100)
        sliderOpacity.setValue(Math.round(element.opacity * 100));
    }

    private void setupPositionSizeSliders(SlideElement element) {
        Slider xSlider = customizationToolbar.findViewById(R.id.slider_x);
        Slider ySlider = customizationToolbar.findViewById(R.id.slider_y);
        Slider widthSlider = customizationToolbar.findViewById(R.id.slider_width);
        Slider heightSlider = customizationToolbar.findViewById(R.id.slider_height);
        TextView xLabel = customizationToolbar.findViewById(R.id.label_x);
        TextView yLabel = customizationToolbar.findViewById(R.id.label_y);
        TextView widthLabel = customizationToolbar.findViewById(R.id.label_width);
        TextView heightLabel = customizationToolbar.findViewById(R.id.label_height);
        xSlider.setValue(element.x);
        ySlider.setValue(element.y);
        widthSlider.setValue(element.width);
        heightSlider.setValue(element.height);
        xLabel.setText(String.format("%.0f%%", element.x * 100));
        yLabel.setText(String.format("%.0f%%", element.y * 100));
        widthLabel.setText(String.format("%.0f%%", element.width * 100));
        heightLabel.setText(String.format("%.0f%%", element.height * 100));
        xSlider.addOnChangeListener((slider, value, fromUser) -> { element.x = value; xLabel.setText(String.format("%.0f%%", value * 100)); slideView.invalidate(); });
        ySlider.addOnChangeListener((slider, value, fromUser) -> { element.y = value; yLabel.setText(String.format("%.0f%%", value * 100)); slideView.invalidate(); });
        widthSlider.addOnChangeListener((slider, value, fromUser) -> { element.width = value; widthLabel.setText(String.format("%.0f%%", value * 100)); slideView.invalidate(); });
        heightSlider.addOnChangeListener((slider, value, fromUser) -> { element.height = value; heightLabel.setText(String.format("%.0f%%", value * 100)); slideView.invalidate(); });
    }
    private void setupOpacitySlider(SlideElement element) {
        Slider opacitySlider = customizationToolbar.findViewById(R.id.slider_opacity);
        if (element instanceof TextElement) {
            opacitySlider.setValue(1.0f); // Not implemented for text, but can be added
        } else if (element instanceof ImageElement) {
            opacitySlider.setValue(1.0f); // Not implemented for image, but can be added
        } else if (element instanceof ShapeElement) {
            opacitySlider.setValue(((ShapeElement) element).opacity);
            opacitySlider.addOnChangeListener((slider, value, fromUser) -> {
                ((ShapeElement) element).opacity = value;
                slideView.invalidate();
            });
        }
    }
    private void setupZOrderControls(SlideElement element) {
        MaterialButton btnBringForward = customizationToolbar.findViewById(R.id.btn_bring_forward);
        MaterialButton btnSendBackward = customizationToolbar.findViewById(R.id.btn_send_backward);
        btnBringForward.setOnClickListener(v -> {
            // Move element up in the elements list
            if (slideRenderer != null) {
                slideRenderer.bringElementForward(element);
                slideView.invalidate();
            }
        });
        btnSendBackward.setOnClickListener(v -> {
            // Move element down in the elements list
            if (slideRenderer != null) {
                slideRenderer.sendElementBackward(element);
                slideView.invalidate();
            }
        });
    }

    private void hideCustomizationToolbar() {
        customizationToolbar.setVisibility(View.GONE);
        selectedElement = null;
        if (slideRenderer != null) {
            slideRenderer.setSelectedElement(null);
        }
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

    private int dpToPx(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
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

    @Override
    public void onImageSelectionRequested(SlideElement element) {
        // Handle image selection - for now, we'll show a placeholder
        // You can implement image picker here in the future
        Toast.makeText(getContext(), "Image selection feature coming soon", Toast.LENGTH_SHORT).show();
    }
}
