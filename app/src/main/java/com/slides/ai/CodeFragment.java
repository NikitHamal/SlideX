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

        saveFab.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCodeSaved(codeInput.getText().toString());
            }
        });

        return view;
    }

    public void setCode(String json) {
        codeInput.setText(json);
    }
}
