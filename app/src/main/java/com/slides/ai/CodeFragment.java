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
    
    private List<String> slideJsonList = new ArrayList<>();
    private int currentSlideIndex = 0;

    public interface CodeInteractionListener {
        void onCodeSaved(String json, int slideIndex);
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
    }

    private void addNewSlide() {
        // Save current slide content first
        saveCurrentSlideContent();
        
        // Add new slide
        int newIndex = slideJsonList.size();
        addSlideToList();
        addTabForSlide(newIndex);
        
        // Switch to new slide
        TabLayout.Tab newTab = slidesTabLayout.getTabAt(newIndex);
        if (newTab != null) {
            newTab.select();
        }
    }

    private void addSlideToList() {
        String defaultJson = generateDefaultSlideJson();
        slideJsonList.add(defaultJson);
    }

    private void addTabForSlide(int index) {
        TabLayout.Tab tab = slidesTabLayout.newTab();
        tab.setText("Slide " + (index + 1));
        slidesTabLayout.addTab(tab);
    }

    private void saveCurrentSlide() {
        saveCurrentSlideContent();
        if (listener != null && currentSlideIndex < slideJsonList.size()) {
            String code = slideJsonList.get(currentSlideIndex);
            if (!code.trim().isEmpty()) {
                listener.onCodeSaved(code, currentSlideIndex);
            }
        }
    }

    private void saveCurrentSlideContent() {
        if (currentSlideIndex < slideJsonList.size()) {
            String content = codeInput.getText().toString().trim();
            if (!content.isEmpty()) {
                slideJsonList.set(currentSlideIndex, content);
            }
        }
    }

    private void loadSlideContent(int index) {
        if (index < slideJsonList.size()) {
            codeInput.setText(slideJsonList.get(index));
        }
    }

    public void setCode(String json) {
        if (codeInput != null) {
            codeInput.setText(json);
        }
        // Update current slide in list
        if (currentSlideIndex < slideJsonList.size()) {
            slideJsonList.set(currentSlideIndex, json);
        }
    }

    public String getCode() {
        return codeInput != null ? codeInput.getText().toString() : "";
    }

    public List<String> getAllSlides() {
        saveCurrentSlideContent();
        return new ArrayList<>(slideJsonList);
    }

    public int getCurrentSlideIndex() {
        return currentSlideIndex;
    }

    public int getSlideCount() {
        return slideJsonList.size();
    }

    private String generateDefaultSlideJson() {
        return "{\n" +
            "  \"backgroundColor\": \"#FFFFFF\",\n" +
            "  \"elements\": [\n" +
            "    {\n" +
            "      \"type\": \"text\",\n" +
            "      \"x\": 50,\n" +
            "      \"y\": 50,\n" +
            "      \"width\": 220,\n" +
            "      \"height\": 40,\n" +
            "      \"text\": \"Slide " + (slideJsonList.size() + 1) + "\",\n" +
            "      \"fontSize\": 24,\n" +
            "      \"color\": \"#333333\",\n" +
            "      \"fontWeight\": \"bold\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"type\": \"text\",\n" +
            "      \"x\": 50,\n" +
            "      \"y\": 100,\n" +
            "      \"width\": 220,\n" +
            "      \"height\": 60,\n" +
            "      \"text\": \"Edit this slide content\",\n" +
            "      \"fontSize\": 16,\n" +
            "      \"color\": \"#666666\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }
}
