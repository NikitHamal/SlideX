package com.slides.ai;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ApiKeyActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ApiKeyAdapter adapter;
    private List<ApiKeyManager.ApiKey> apiKeys;
    private ApiKeyManager apiKeyManager;
    private View emptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_api_key);

        recyclerView = findViewById(R.id.api_key_recycler_view);
        emptyState = findViewById(R.id.empty_state);
        FloatingActionButton addFab = findViewById(R.id.add_api_key_fab);

        apiKeyManager = new ApiKeyManager(this);
        apiKeys = apiKeyManager.getApiKeyObjects();

        adapter = new ApiKeyAdapter(apiKeys);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        addFab.setOnClickListener(v -> showAddApiKeyDialog());

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

                    apiKeyManager.addApiKey(label, key);
                    apiKeys.clear();
                    apiKeys.addAll(apiKeyManager.getApiKeyObjects());
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
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
                    new MaterialAlertDialogBuilder(ApiKeyActivity.this)
                            .setTitle("Delete API Key")
                            .setMessage("Are you sure you want to delete this API key?")
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
