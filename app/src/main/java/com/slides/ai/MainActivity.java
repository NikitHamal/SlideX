package com.slides.ai;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private SlideStackAdapter adapter;
    private List<SlideStack> slideStacks;
    private SharedPreferences sharedPreferences;
    private ExtendedFloatingActionButton fabCreate;
    private MaterialToolbar toolbar;
    private View emptyStateView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slide_stacks);

        initViews();
        setupRecyclerView();
        loadSlideStacks();
        updateEmptyState();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.recyclerView);
        fabCreate = findViewById(R.id.fabCreate);
        emptyStateView = findViewById(R.id.emptyStateView);

        setSupportActionBar(toolbar);

        sharedPreferences = getSharedPreferences("slide_stacks", MODE_PRIVATE);

        fabCreate.setOnClickListener(v -> createNewSlideStack());

        // Setup menu items in toolbar
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            return false;
        });
    }

    private void setupRecyclerView() {
        slideStacks = new ArrayList<>();
        adapter = new SlideStackAdapter(slideStacks);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);
    }

    private void loadSlideStacks() {
        slideStacks.clear();
        String stacksJson = sharedPreferences.getString("stacks", "[]");

        try {
            JSONArray stacksArray = new JSONArray(stacksJson);
            for (int i = 0; i < stacksArray.length(); i++) {
                JSONObject stackObj = stacksArray.getJSONObject(i);
                SlideStack stack = SlideStack.fromJson(stackObj);
                slideStacks.add(stack);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void createNewSlideStack() {
        // Simply open the slide editor with a temporary stack
        // The stack will only be saved when it has actual content
        String stackId = "temp_" + System.currentTimeMillis();
        String stackName = "New Slide Stack";
        SlideStack tempStack = new SlideStack(stackId, stackName, new ArrayList<>(), System.currentTimeMillis());

        // Open the slide editor for this temporary stack
        openSlideEditor(tempStack);
    }

    private void saveSlideStacks() {
        JSONArray stacksArray = new JSONArray();
        for (SlideStack stack : slideStacks) {
            stacksArray.put(stack.toJson());
        }

        sharedPreferences.edit()
                .putString("stacks", stacksArray.toString())
                .apply();
    }

    private void openSlideEditor(SlideStack stack) {
        Intent intent = new Intent(this, SlideActivity.class);
        intent.putExtra("stack_id", stack.getId());
        intent.putExtra("stack_name", stack.getName());
        startActivity(intent);
    }

    private void updateEmptyState() {
        if (slideStacks.isEmpty()) {
            emptyStateView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSlideStacks(); // Reload in case data was modified
    }

    private class SlideStackAdapter extends RecyclerView.Adapter<SlideStackAdapter.ViewHolder> {
        private List<SlideStack> stacks;

        public SlideStackAdapter(List<SlideStack> stacks) {
            this.stacks = stacks;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_slide_stack, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SlideStack stack = stacks.get(position);
            holder.bind(stack);
        }

        @Override
        public int getItemCount() {
            return stacks.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private MaterialCardView cardView;
            private TextView titleText;
            private TextView slideCountText;
            private TextView lastModifiedText;
            private ImageView previewImage;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                cardView = (MaterialCardView) itemView;
                titleText = itemView.findViewById(R.id.titleText);
                slideCountText = itemView.findViewById(R.id.slideCountText);
                lastModifiedText = itemView.findViewById(R.id.lastModifiedText);
                previewImage = itemView.findViewById(R.id.previewImage);

                cardView.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        openSlideEditor(stacks.get(position));
                    }
                });
            }

            public void bind(SlideStack stack) {
                titleText.setText(stack.getName());
                slideCountText.setText(stack.getSlides().size() + " slides");

                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                lastModifiedText.setText("Modified " + sdf.format(new Date(stack.getLastModified())));

                // TODO: Generate preview image from first slide
                previewImage.setImageResource(R.drawable.ic_slides_preview);
            }
        }
    }

    public static class SlideStack {
        private String id;
        private String name;
        private List<String> slides; // JSON strings of slides
        private long lastModified;

        public SlideStack(String id, String name, List<String> slides, long lastModified) {
            this.id = id;
            this.name = name;
            this.slides = slides;
            this.lastModified = lastModified;
        }

        public JSONObject toJson() {
            try {
                JSONObject obj = new JSONObject();
                obj.put("id", id);
                obj.put("name", name);
                obj.put("lastModified", lastModified);

                JSONArray slidesArray = new JSONArray();
                for (String slide : slides) {
                    slidesArray.put(slide);
                }
                obj.put("slides", slidesArray);

                return obj;
            } catch (JSONException e) {
                e.printStackTrace();
                return new JSONObject();
            }
        }

        public static SlideStack fromJson(JSONObject obj) {
            try {
                String id = obj.getString("id");
                String name = obj.getString("name");
                long lastModified = obj.getLong("lastModified");

                List<String> slides = new ArrayList<>();
                JSONArray slidesArray = obj.getJSONArray("slides");
                for (int i = 0; i < slidesArray.length(); i++) {
                    slides.add(slidesArray.getString(i));
                }

                return new SlideStack(id, name, slides, lastModified);
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }

        // Getters
        public String getId() { return id; }
        public String getName() { return name; }
        public List<String> getSlides() { return slides; }
        public long getLastModified() { return lastModified; }

        // Setters
        public void setName(String name) { this.name = name; }
        public void setLastModified(long lastModified) { this.lastModified = lastModified; }
    }
}