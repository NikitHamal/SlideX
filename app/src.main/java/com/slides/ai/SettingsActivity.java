package com.slides.ai;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.slides.ai.qwen.auth.TokenManager;

import java.util.List;

public class SettingsActivity extends AppCompatActivity {
    private MaterialToolbar toolbar;
    private MaterialCardView themeCard;
    private MaterialCardView apiKeysCard;
    private MaterialCardView qwenLoginCard;
    private MaterialCardView aboutCard;
    private AutoCompleteTextView themeSpinner;
    private TextView apiKeysCountText;
    private TextView qwenLoginStatusText;
    private ApiKeyManager apiKeyManager;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initViews();
        setupTheme();
        setupApiKeys();
        setupQwenLogin();
        setupAbout();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        themeCard = findViewById(R.id.themeCard);
        apiKeysCard = findViewById(R.id.apiKeysCard);
        qwenLoginCard = findViewById(R.id.qwenLoginCard);
        aboutCard = findViewById(R.id.aboutCard);
        findViewById(R.id.documentationCard).setOnClickListener(v -> {
            startActivity(new Intent(this, DocumentationActivity.class));
        });
        themeSpinner = findViewById(R.id.themeSpinner);
        apiKeysCountText = findViewById(R.id.apiKeysCountText);
        qwenLoginStatusText = findViewById(R.id.qwenLoginStatusText);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }

        toolbar.setNavigationOnClickListener(v -> finish());

        apiKeyManager = new ApiKeyManager(this);
        tokenManager = new TokenManager(this);
    }

    private void setupTheme() {
        String[] themeOptions = {"Light", "Dark", "System Default"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, themeOptions);
        themeSpinner.setAdapter(adapter);

        int currentTheme = ThemeManager.getThemeMode(this);
        themeSpinner.setText(ThemeManager.getThemeName(currentTheme), false);

        themeSpinner.setOnItemClickListener((parent, view, position, id) -> {
            ThemeManager.setThemeMode(this, position);
            // Recreate activity to apply theme immediately
            recreate();
        });
    }

    private void setupApiKeys() {
        updateApiKeysCount();

        apiKeysCard.setOnClickListener(v -> {
            startActivity(new Intent(this, ApiKeyActivity.class));
        });
    }

    private void updateApiKeysCount() {
        List<ApiKeyManager.ApiKey> keys = apiKeyManager.getApiKeyObjects();
        String countText = keys.size() + " API key" + (keys.size() != 1 ? "s" : "");
        apiKeysCountText.setText(countText);
    }

    private void setupQwenLogin() {
        updateQwenLoginStatus();

        qwenLoginCard.setOnClickListener(v -> {
            if (tokenManager.isTokenValid()) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Logout from Qwen")
                        .setMessage("Are you sure you want to log out? You will need to log in again to use Qwen models.")
                        .setPositiveButton("Logout", (dialog, which) -> {
                            tokenManager.clearToken();
                            updateQwenLoginStatus();
                            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            } else {
                Toast.makeText(this, "You are not logged in. Go to the Chat tab to log in.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateQwenLoginStatus() {
        if (tokenManager.isTokenValid()) {
            qwenLoginStatusText.setText("Logged In");
        } else {
            qwenLoginStatusText.setText("Not Logged In");
        }
    }


    private void setupAbout() {
        aboutCard.setOnClickListener(v -> showAboutDialog());
    }

    private void showAboutDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_about, null);

        builder.setView(dialogView)
                .setPositiveButton("OK", null)
                .show();
    }
}
