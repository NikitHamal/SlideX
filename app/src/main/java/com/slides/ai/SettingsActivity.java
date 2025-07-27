package com.slides.ai;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {
    private MaterialToolbar toolbar;
    private MaterialCardView themeCard;
    private MaterialCardView apiKeysCard;
    private MaterialCardView aboutCard;
    private AutoCompleteTextView themeSpinner;
    private TextView apiKeysCountText;
    private ApiKeyManager apiKeyManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initViews();
        setupTheme();
        setupApiKeys();
        setupAbout();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        themeCard = findViewById(R.id.themeCard);
        apiKeysCard = findViewById(R.id.apiKeysCard);
        aboutCard = findViewById(R.id.aboutCard);
        findViewById(R.id.documentationCard).setOnClickListener(v -> {
            startActivity(new Intent(this, DocumentationActivity.class));
        });
        themeSpinner = findViewById(R.id.themeSpinner);
        apiKeysCountText = findViewById(R.id.apiKeysCountText);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }

        toolbar.setNavigationOnClickListener(v -> finish());

        apiKeyManager = new ApiKeyManager(this);
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

        apiKeysCard.setOnClickListener(v -> showApiKeysBottomSheet());
    }

    private void updateApiKeysCount() {
        List<ApiKeyManager.ApiKey> keys = apiKeyManager.getApiKeyObjects();
        String countText = keys.size() + " API key" + (keys.size() != 1 ? "s" : "");
        apiKeysCountText.setText(countText);
    }

    private void showApiKeysBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this,
                R.style.ThemeOverlay_Material3_BottomSheetDialog);

        View bottomSheetView = LayoutInflater.from(this)
                .inflate(R.layout.bottom_sheet_api_keys, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        RecyclerView recyclerView = bottomSheetView.findViewById(R.id.recyclerView);
        FloatingActionButton fabAdd = bottomSheetView.findViewById(R.id.fabAdd);
        TextView titleText = bottomSheetView.findViewById(R.id.titleText);

        titleText.setText("API Keys");

        ApiKeyAdapter adapter = new ApiKeyAdapter(apiKeyManager.getApiKeyObjects());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            showAddApiKeyDialog();
        });

        bottomSheetDialog.show();
    }

    private void showAddApiKeyDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_api_key, null);

        TextInputLayout labelLayout = dialogView.findViewById(R.id.labelLayout);
        TextInputLayout keyLayout = dialogView.findViewById(R.id.keyLayout);
        TextInputEditText labelEdit = dialogView.findViewById(R.id.labelEdit);
        TextInputEditText keyEdit = dialogView.findViewById(R.id.keyEdit);

        builder.setTitle("Add API Key")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String label = labelEdit.getText().toString().trim();
                    String key = keyEdit.getText().toString().trim();

                    if (key.isEmpty()) {
                        Toast.makeText(this, "API key cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    apiKeyManager.addApiKey(key, label);
                    updateApiKeysCount();
                    Toast.makeText(this, "API key added successfully", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
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

    private class ApiKeyAdapter extends RecyclerView.Adapter<ApiKeyAdapter.ViewHolder> {
        private List<ApiKeyManager.ApiKey> keys;

        public ApiKeyAdapter(List<ApiKeyManager.ApiKey> keys) {
            this.keys = keys;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_api_key, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ApiKeyManager.ApiKey key = keys.get(position);
            holder.bind(key);
        }

        @Override
        public int getItemCount() {
            return keys.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private TextView labelText;
            private TextView keyText;
            private TextView dateText;
            private SwitchMaterial visibilitySwitch;
            private ImageView deleteButton;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                labelText = itemView.findViewById(R.id.labelText);
                keyText = itemView.findViewById(R.id.keyText);
                dateText = itemView.findViewById(R.id.dateText);
                visibilitySwitch = itemView.findViewById(R.id.visibilitySwitch);
                deleteButton = itemView.findViewById(R.id.deleteButton);
            }

            public void bind(ApiKeyManager.ApiKey key) {
                labelText.setText(key.getLabel());
                keyText.setText(key.getDisplayKey());

                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                dateText.setText("Added " + sdf.format(new Date(key.getCreatedAt())));

                visibilitySwitch.setChecked(key.isVisible());
                visibilitySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    apiKeyManager.updateApiKey(key.getId(), key.getLabel(), isChecked);
                    keyText.setText(key.getDisplayKey());
                });

                deleteButton.setOnClickListener(v -> {
                    new MaterialAlertDialogBuilder(SettingsActivity.this)
                            .setTitle("Delete API Key")
                            .setMessage("Are you sure you want to delete this API key?")
                            .setPositiveButton("Delete", (dialog, which) -> {
                                apiKeyManager.removeApiKey(key.getId());
                                keys.remove(getAdapterPosition());
                                notifyItemRemoved(getAdapterPosition());
                                updateApiKeysCount();
                                Toast.makeText(SettingsActivity.this, "API key deleted", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                });
            }
        }
    }
}