package com.slides.ai;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.widget.EditText;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class CodeFragment extends Fragment {

    private EditText codeInput;
    private FloatingActionButton saveFab;
    private TabLayout slidesTabLayout;
    private MaterialButton addSlideButton;
    private CodeInteractionListener listener;
    
    private List<String> slideHtmlList = new ArrayList<>();
    private int currentSlideIndex = 0;

    public interface CodeInteractionListener {
        void onCodeSaved(String html, int slideIndex);
        void onSlideChanged(int slideIndex);
    }

    public void setCodeInteractionListener(CodeInteractionListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_code, container, false);

        initViews(view);
        setupTabs();
        setupDefaultSlide();

        return view;
    }

    private void initViews(View view) {
        codeInput = view.findViewById(R.id.code_input);
        saveFab = view.findViewById(R.id.save_fab);
        slidesTabLayout = view.findViewById(R.id.slides_tab_layout);
        addSlideButton = view.findViewById(R.id.add_slide_button);

        saveFab.setOnClickListener(v -> saveCurrentSlide());
        addSlideButton.setOnClickListener(v -> addNewSlide());
    }

    private void setupTabs() {
        slidesTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                saveCurrentSlideContent();
                currentSlideIndex = tab.getPosition();
                loadSlideContent(currentSlideIndex);
                if (listener != null) {
                    listener.onSlideChanged(currentSlideIndex);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Save content when leaving tab
                saveCurrentSlideContent();
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Do nothing
            }
        });
    }

    private void setupDefaultSlide() {
        // Initialize with first slide
        addSlideToList();
        addTabForSlide(0);
        loadSlideContent(0);
    }

    public void addNewSlide() {
        // Save current slide content first
        saveCurrentSlideContent();
        
        // Add new slide
        int newIndex = slideHtmlList.size();
        addSlideToList();
        addTabForSlide(newIndex);
        
        // Switch to new slide
        TabLayout.Tab newTab = slidesTabLayout.getTabAt(newIndex);
        if (newTab != null) {
            newTab.select();
        }
    }

    public void addNewSlideFromExternal() {
        // Called from external sources (like slides fragment plus button)
        addNewSlide();
    }

    public void addSlideFromHtml(String htmlCode) {
        // Add a slide with specific HTML content
        saveCurrentSlideContent();
        
        int newIndex = slideHtmlList.size();
        slideHtmlList.add(htmlCode);
        addTabForSlide(newIndex);
        
        // Switch to new slide
        TabLayout.Tab newTab = slidesTabLayout.getTabAt(newIndex);
        if (newTab != null) {
            newTab.select();
        }
        
        // Load the content
        loadSlideContent(newIndex);
    }

    private void addSlideToList() {
        String defaultHtml = generateDefaultSlideHtml();
        slideHtmlList.add(defaultHtml);
    }

    private void addTabForSlide(int index) {
        TabLayout.Tab tab = slidesTabLayout.newTab();
        tab.setText("Slide " + (index + 1));
        slidesTabLayout.addTab(tab);
    }

    private void saveCurrentSlide() {
        saveCurrentSlideContent();
        if (listener != null && currentSlideIndex < slideHtmlList.size()) {
            String code = slideHtmlList.get(currentSlideIndex);
            if (!code.trim().isEmpty()) {
                listener.onCodeSaved(code, currentSlideIndex);
            }
        }
    }

    private void saveCurrentSlideContent() {
        if (currentSlideIndex < slideHtmlList.size() && codeInput != null) {
            String content = codeInput.getText().toString().trim();
            if (!content.isEmpty()) {
                slideHtmlList.set(currentSlideIndex, content);
            }
        }
    }

    private void loadSlideContent(int index) {
        if (index < slideHtmlList.size() && codeInput != null) {
            codeInput.setText(slideHtmlList.get(index));
        }
    }

    public void setCode(String html) {
        if (codeInput != null) {
            codeInput.setText(html);
        }
        // Update current slide in list
        if (currentSlideIndex < slideHtmlList.size()) {
            slideHtmlList.set(currentSlideIndex, html);
        }
    }

    public String getCode() {
        saveCurrentSlideContent(); // Ensure current content is saved
        return codeInput != null ? codeInput.getText().toString() : "";
    }

    public List<String> getAllSlides() {
        saveCurrentSlideContent();
        return new ArrayList<>(slideHtmlList);
    }

    public int getCurrentSlideIndex() {
        return currentSlideIndex;
    }

    public int getSlideCount() {
        return slideHtmlList.size();
    }

    public boolean isCurrentSlideDefault() {
        if (currentSlideIndex == 0 && getSlideCount() == 1) {
            String currentCode = getCode().trim();
            String defaultCode = generateDefaultSlideHtml().trim();
            // We need to compare the content without the slide number, as it can change
            return currentCode.contains("<h2>Slide 1</h2>");
        }
        return false;
    }

    public void navigateToSlide(int index) {
        if (index >= 0 && index < slideHtmlList.size()) {
            TabLayout.Tab tab = slidesTabLayout.getTabAt(index);
            if (tab != null) {
                tab.select();
            }
        }
    }

    private String generateDefaultSlideHtml() {
        return "<section>\n" +
            "  <h2>Slide " + (slideHtmlList.size() + 1) + "</h2>\n" +
            "  <p>Edit this slide content</p>\n" +
            "</section>";
    }
}
