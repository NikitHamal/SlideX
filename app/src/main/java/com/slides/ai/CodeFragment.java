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

public class CodeFragment extends Fragment {

    private EditText codeInput;
    private FloatingActionButton saveFab;
    private CodeInteractionListener listener;

    public interface CodeInteractionListener {
        void onCodeSaved(String json);
    }

    public void setCodeInteractionListener(CodeInteractionListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_code, container, false);

        codeInput = view.findViewById(R.id.code_input);
        saveFab = view.findViewById(R.id.save_fab);

        // Set default sample JSON
        setDefaultSampleCode();

        saveFab.setOnClickListener(v -> {
            if (listener != null) {
                String code = codeInput.getText().toString().trim();
                if (!code.isEmpty()) {
                    listener.onCodeSaved(code);
                } else {
                    // Show error or set default
                    setDefaultSampleCode();
                    listener.onCodeSaved(codeInput.getText().toString());
                }
            }
        });

        return view;
    }

    public void setCode(String json) {
        if (codeInput != null) {
            codeInput.setText(json);
        }
    }

    public String getCode() {
        return codeInput != null ? codeInput.getText().toString() : "";
    }

    private void setDefaultSampleCode() {
        String sampleJson = "{\n" +
            "  \"backgroundColor\": \"#FFFFFF\",\n" +
            "  \"elements\": [\n" +
            "    {\n" +
            "      \"type\": \"text\",\n" +
            "      \"x\": 50,\n" +
            "      \"y\": 50,\n" +
            "      \"width\": 220,\n" +
            "      \"height\": 40,\n" +
            "      \"text\": \"Welcome to Slides AI\",\n" +
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
            "      \"text\": \"Create beautiful presentations with JSON\",\n" +
            "      \"fontSize\": 16,\n" +
            "      \"color\": \"#666666\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
        
        if (codeInput != null) {
            codeInput.setText(sampleJson);
        }
    }
}
