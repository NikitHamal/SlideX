package com.slides.ai;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ApiKeyActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private RecyclerView recyclerView;
    private ApiKeyAdapter adapter;
    private List<ApiKeyManager.ApiKey> apiKeys;
    private ApiKeyManager apiKeyManager;
    private View emptyState;
    
    // Qwen token section
    private MaterialCardView qwenTokenCard;
    private TextView qwenTokenStatus;
    private MaterialButton btnAddQwenToken;
    private MaterialButton btnRemoveQwenToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_api_key);

        initViews();
        setupRecyclerView();
        setupQwenTokenSection();
        loadApiKeys();
        updateEmptyState();
        updateQwenTokenStatus();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.api_key_recycler_view);
        emptyState = findViewById(R.id.empty_state);
        FloatingActionButton addFab = findViewById(R.id.add_api_key_fab);
        
        // Qwen token views
        qwenTokenCard = findViewById(R.id.qwen_token_card);
        qwenTokenStatus = findViewById(R.id.qwen_token_status);
        btnAddQwenToken = findViewById(R.id.btn_add_qwen_token);
        btnRemoveQwenToken = findViewById(R.id.btn_remove_qwen_token);

        // Setup toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("API Keys");
        }
        
        // Handle toolbar navigation
        toolbar.setNavigationOnClickListener(v -> finish());

        apiKeyManager = new ApiKeyManager(this);
        addFab.setOnClickListener(v -> showAddApiKeyDialog());
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        // Add item spacing for better Material 3 appearance
        recyclerView.addItemDecoration(new androidx.recyclerview.widget.RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(android.graphics.Rect outRect, android.view.View view, 
                    androidx.recyclerview.widget.RecyclerView parent, 
                    androidx.recyclerview.widget.RecyclerView.State state) {
                outRect.bottom = 8;
            }
        });
    }

    private void setupQwenTokenSection() {
        btnAddQwenToken.setOnClickListener(v -> showAddQwenTokenDialog());
        btnRemoveQwenToken.setOnClickListener(v -> showRemoveQwenTokenDialog());
    }

    private void updateQwenTokenStatus() {
        if (apiKeyManager.hasQwenToken()) {
            qwenTokenStatus.setText("Qwen token configured");
            btnAddQwenToken.setText("Update Token");
            btnRemoveQwenToken.setVisibility(View.VISIBLE);
        } else {
            qwenTokenStatus.setText("No Qwen token configured");
            btnAddQwenToken.setText("Add Token");
            btnRemoveQwenToken.setVisibility(View.GONE);
        }
    }

    private void showAddQwenTokenDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this,
                R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_qwen_token, null);

        TextInputEditText tokenEdit = dialogView.findViewById(R.id.tokenEdit);
        
        // Pre-fill with existing token if available
        String existingToken = apiKeyManager.getQwenToken();
        if (existingToken != null && !existingToken.isEmpty()) {
            tokenEdit.setText(existingToken);
        }

        builder.setTitle(apiKeyManager.hasQwenToken() ? "Update Qwen Token" : "Add Qwen Token")
                .setMessage("Enter your Qwen chat token to use Qwen models")
                .setView(dialogView)
                .setPositiveButton(apiKeyManager.hasQwenToken() ? "Update" : "Add", (dialog, which) -> {
                    String token = tokenEdit.getText().toString().trim();

                    if (token.isEmpty()) {
                        Toast.makeText(this, "Token cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    apiKeyManager.saveQwenToken(token);
                    updateQwenTokenStatus();
                    Toast.makeText(this, "Qwen token saved successfully", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRemoveQwenTokenDialog() {
        new MaterialAlertDialogBuilder(this,
                R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered)
                .setTitle("Remove Qwen Token")
                .setMessage("Are you sure you want to remove the Qwen token? You won't be able to use Qwen models without it.")
                .setPositiveButton("Remove", (dialog, which) -> {
                    apiKeyManager.removeQwenToken();
                    updateQwenTokenStatus();
                    Toast.makeText(this, "Qwen token removed", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadApiKeys() {
        apiKeys = apiKeyManager.getApiKeyObjects();
        adapter = new ApiKeyAdapter(apiKeys);
        recyclerView.setAdapter(adapter);
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (apiKeys.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showAddApiKeyDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this,
                R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_api_key, null);

        TextInputLayout labelLayout = dialogView.findViewById(R.id.labelLayout);
        TextInputLayout keyLayout = dialogView.findViewById(R.id.keyLayout);
        TextInputEditText labelEdit = dialogView.findViewById(R.id.labelEdit);
        TextInputEditText keyEdit = dialogView.findViewById(R.id.keyEdit);

        builder.setTitle("Add Gemini API Key")
                .setMessage("Enter your Gemini API key details to use Gemini models")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String label = labelEdit.getText().toString().trim();
                    String key = keyEdit.getText().toString().trim();

                    if (key.isEmpty()) {
                        Toast.makeText(this, "API key cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (label.isEmpty()) {
                        label = "Gemini API Key " + (apiKeys.size() + 1);
                    }

                    apiKeyManager.addApiKey(key, label);
                    loadApiKeys(); // Reload the list
                    Toast.makeText(this, "API key added successfully", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
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
            private ImageView deleteButton;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                labelText = itemView.findViewById(R.id.labelText);
                keyText = itemView.findViewById(R.id.keyText);
                dateText = itemView.findViewById(R.id.dateText);
                deleteButton = itemView.findViewById(R.id.deleteButton);
            }

            public void bind(ApiKeyManager.ApiKey key) {
                labelText.setText(key.getLabel());
                keyText.setText(key.getDisplayKey());

                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                dateText.setText("Added " + sdf.format(new Date(key.getCreatedAt())));

                deleteButton.setOnClickListener(v -> {
                    new MaterialAlertDialogBuilder(ApiKeyActivity.this,
                            R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered)
                            .setTitle("Delete API Key")
                            .setMessage("Are you sure you want to delete \"" + key.getLabel() + "\"? This action cannot be undone.")
                            .setPositiveButton("Delete", (dialog, which) -> {
                                apiKeyManager.removeApiKey(key.getId());
                                keys.remove(getAdapterPosition());
                                notifyItemRemoved(getAdapterPosition());
                                updateEmptyState();
                                Toast.makeText(ApiKeyActivity.this, "API key deleted", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                });
            }
        }
    }
}
