package com.slides.ai;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class SlidesFragment extends Fragment {

    private WebView slideWebView;
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
        updateNavigationControls();
        return view;
    }

    private void initViews(View view) {
        slideWebView = view.findViewById(R.id.slide_webview);
        WebSettings webSettings = slideWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            slideWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        } else {
            slideWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        btnPreviousSlide = view.findViewById(R.id.btn_previous_slide);
        btnNextSlide = view.findViewById(R.id.btn_next_slide);
        btnAddSlide = view.findViewById(R.id.btn_add_slide);
        slideCounter = view.findViewById(R.id.slide_counter);

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

    public void setSlideHtml(String html) {
        if (slideWebView != null) {
            slideWebView.loadDataWithBaseURL("file:///android_res/raw/", html, "text/html", "UTF-8", null);
        }
    }

    public void setSlides(List<JSONObject> slideList) {
        this.slides = new ArrayList<>(slideList);
        if (currentSlideIndex >= slides.size()) {
            currentSlideIndex = Math.max(0, slides.size() - 1);
        }
        updateNavigationControls();
    }

    public void navigateToSlide(int index) {
        if (index >= 0 && index < slides.size()) {
            currentSlideIndex = index;
            if (slideWebView != null) {
                slideWebView.evaluateJavascript("Reveal.slide(" + index + ");", null);
            }
            updateNavigationControls();
            if (navigationListener != null) {
                navigationListener.onSlideChanged(index);
            }
        }
    }

    private void updateNavigationControls() {
        int slideCount = slides.size();
        slideCounter.setText((currentSlideIndex + 1) + "/" + slideCount);
        btnPreviousSlide.setVisibility(slideCount > 1 && currentSlideIndex > 0 ? View.VISIBLE : View.GONE);
        btnNextSlide.setVisibility(slideCount > 1 && currentSlideIndex < slideCount - 1 ? View.VISIBLE : View.GONE);
    }

    public int getCurrentSlideIndex() {
        return currentSlideIndex;
    }

    public int getSlideCount() {
        return slides.size();
    }
}
