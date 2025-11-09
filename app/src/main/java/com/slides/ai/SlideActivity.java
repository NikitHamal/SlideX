package com.slides.ai;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.Manifest;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Toast;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;
import com.google.android.material.switchmaterial.SwitchMaterial;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.pdf.PdfDocument;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.IOException;

import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.slides.ai.qwen.QwenManager;

public class SlideActivity extends AppCompatActivity implements
        CodeFragment.CodeInteractionListener,
        SlidesFragment.SlideNavigationListener, ChatFragment.ChatInteractionListener {

	private ViewPager2 viewPager;
	private TabLayout tabLayout;
	private ViewPagerAdapter adapter;

	private final String API_KEY = "Gemini_API";
	private final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";
	private HashMap<String, Bitmap> imageCache = new HashMap<>();
	private Handler mainHandler;
	private ExecutorService executorService;

	private NetworkManager networkManager;
	private QwenManager qwenManager;
	private ApiKeyManager apiKeyManager;
	
	// Stack information for saving
	private String stackId;
	private String stackName;

	// Fragment references
	private SlidesFragment slidesFragment;
	private CodeFragment codeFragment;
	private ChatFragment chatFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_slide);

		// Get stack information from intent
		Intent intent = getIntent();
		stackId = intent.getStringExtra("stack_id");
		stackName = intent.getStringExtra("stack_name");

		initViews();
		setupFragments();

		mainHandler = new Handler();
		executorService = Executors.newCachedThreadPool();

		apiKeyManager = new ApiKeyManager(this);
		networkManager = new NetworkManager(apiKeyManager, imageCache, mainHandler, executorService);
		qwenManager = new QwenManager(apiKeyManager, mainHandler, executorService);
	}

	private void initViews() {
		tabLayout = findViewById(R.id.tab_layout);
		viewPager = findViewById(R.id.view_pager);

		// Setup toolbar
		setSupportActionBar(findViewById(R.id.toolbar));
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			getSupportActionBar().setTitle(stackName != null ? stackName : "Slide Editor");
		}
	}

	private void setupFragments() {
		adapter = new ViewPagerAdapter(this);
		viewPager.setAdapter(adapter);
        viewPager.setUserInputEnabled(false);

		new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
			switch (position) {
				case 0:
					tab.setText("Slides");
					break;
				case 1:
					tab.setText("Chat");
					break;
				case 2:
					tab.setText("Code");
					break;
			}
		}).attach();

		// Setup fragment communication after ViewPager is configured
		setupFragmentCommunication();
	}

	private void setupFragmentCommunication() {
		// Use a more reliable way to get fragment references
		viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
			@Override
			public void onPageSelected(int position) {
				super.onPageSelected(position);
				// Ensure fragments are initialized when first accessed
				ensureFragmentReferences();
			}
		});

		// Initial setup with delay to ensure fragments are created
		viewPager.post(() -> {
			ensureFragmentReferences();
		});
	}

	private void ensureFragmentReferences() {
		// Get fragment references from the adapter
		slidesFragment = adapter.getSlidesFragment();
		codeFragment = adapter.getCodeFragment();
		chatFragment = adapter.getChatFragment();

		// Set interaction listeners if fragments are available
		if (codeFragment != null) {
			codeFragment.setCodeInteractionListener(this);
		}
		if (slidesFragment != null) {
			slidesFragment.setSlideNavigationListener(this);
		}
		if (chatFragment != null) {
			chatFragment.setChatInteractionListener(this);
		}
	}

	@Override
	public void onCodeSaved(String html, int slideIndex) {
		ensureFragmentReferences();
		if (slidesFragment != null && codeFragment != null) {
			List<String> allSlides = codeFragment.getAllSlides();
			RevealJsGenerator generator = new RevealJsGenerator(this);
			String fullHtml = generator.generateHtml(allSlides);
			slidesFragment.setSlideHtml(fullHtml);
		}

		// Save the slide stack if it's temporary
		saveSlideStackIfTemporary();

		// Switch to slides tab to show the rendered slide
		viewPager.setCurrentItem(0);

		Toast.makeText(this, "Slide " + (slideIndex + 1) + " updated successfully", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onSlideChanged(int slideIndex) {
		// Sync code fragment to show the correct slide
		ensureFragmentReferences();
		if (codeFragment != null) {
			// The code fragment should handle tab selection automatically
			// We could add additional sync logic here if needed
		}
	}

	@Override
	public void onAddSlideRequested() {
		// Switch to code tab and add a new slide
		ensureFragmentReferences();
		if (codeFragment != null) {
			viewPager.setCurrentItem(2); // Switch to code tab
			// The add slide functionality is handled in the code fragment
			// Trigger add slide after switching tabs
			viewPager.post(() -> codeFragment.addNewSlideFromExternal());
		}
	}

	@Override
	public void onChatPromptSent(String prompt, float canvasWidth, float canvasHeight) {
        ensureFragmentReferences();
        String selectedModel = chatFragment.getSelectedModel();

		if (selectedModel.startsWith("gemini")) {
            if (networkManager != null) {
                networkManager.sendPromptToGemini(prompt, 0, 0, new NetworkManager.ApiResponseCallback() {
                    @Override
                    public void onSuccess(String htmlResponse) {
                        try {
                            String html = extractHtmlFromResponse(htmlResponse);
                            handleSuccessfulResponse(html);
                        } catch (Exception e) {
                            Log.e("SlideActivity", "Error extracting HTML from Gemini response", e);
                            handleErrorResponse("Error extracting HTML from Gemini response: " + e.getMessage() + "\n\n" + htmlResponse);
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        handleErrorResponse(errorMessage);
                    }
                });
            } else {
                handleErrorResponse("Network manager not available.");
            }
        } else if (selectedModel.startsWith("qwen") || selectedModel.startsWith("qwq")) {
            if (qwenManager != null) {
                qwenManager.createNewChat(new QwenManager.QwenCallback<com.slides.ai.qwen.QwenNewChatResponse>() {
                    @Override
                    public void onSuccess(com.slides.ai.qwen.QwenNewChatResponse response) {
                        if (response != null && response.success && response.data != null) {
                            qwenManager.getCompletion(response.data.id, null, prompt, selectedModel, 0, 0, new QwenManager.QwenCallback<String>() {
                                @Override
                                public void onSuccess(String htmlResponse) {
                                    try {
                                        String html = extractHtmlFromResponse(htmlResponse);
                                        handleSuccessfulResponse(html);
                                    } catch (Exception e) {
                                        Log.e("SlideActivity", "Error extracting HTML from Qwen response", e);
                                        handleErrorResponse("Error extracting HTML from Qwen response: " + e.getMessage() + "\n\n" + htmlResponse);
                                    }
                                }

                                @Override
                                public void onError(String error) {
                                    Log.e("SlideActivity", "Qwen completion error: " + error);
                                    handleErrorResponse("Qwen API error: " + error);
                                }
                            });
                        } else {
                            Log.e("SlideActivity", "Qwen new chat response invalid: " + (response != null ? response.toString() : "null"));
                            handleErrorResponse("Error creating new Qwen chat session.");
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("SlideActivity", "Qwen new chat error: " + error);
                        handleErrorResponse("Qwen API error: " + error);
                    }
                });
            } else {
                Log.e("SlideActivity", "QwenManager is null");
                handleErrorResponse("Qwen manager not available. Please restart the app.");
            }
        }
	}

    private void handleSuccessfulResponse(String html) {
        ensureFragmentReferences();
        if (codeFragment != null) {
            if (codeFragment.isCurrentSlideDefault()) {
                codeFragment.setCode(html);
                onCodeSaved(html, 0);
            } else {
                codeFragment.addSlideFromHtml(html);
            }

            // Update slides fragment
            if (slidesFragment != null) {
                List<String> allSlides = codeFragment.getAllSlides();
                RevealJsGenerator generator = new RevealJsGenerator(this);
                String fullHtml = generator.generateHtml(allSlides);
                slidesFragment.setSlideHtml(fullHtml);
                slidesFragment.navigateToSlide(allSlides.size() - 1); // Navigate to new slide
            }
        }

        // Switch to slides tab to show the result
        viewPager.setCurrentItem(0);

        // Add AI response to chat
        if (chatFragment != null) {
            chatFragment.addAiResponse("Great! I've created a slide based on your request. You can view it in the Slides tab and edit the HTML code in the Code tab if needed.");
        }
    }

    private void handleErrorResponse(String errorMessage) {
        Log.e("SlideActivity", "Chat API error: " + errorMessage);
        if (chatFragment != null) {
            chatFragment.addAiResponse("An error occurred: " + errorMessage);
        }
    }

    private String extractHtmlFromResponse(String response) {
        // First try to find HTML wrapped in markdown code blocks
        if (response.contains("```html")) {
            int startIdx = response.indexOf("```html") + 7;
            int endIdx = response.lastIndexOf("```");
            if (endIdx > startIdx) {
                return response.substring(startIdx, endIdx).trim();
            }
        }

        // Try to find a <section> tag
        int startIdx = response.indexOf("<section");
        if (startIdx != -1) {
            int endIdx = response.lastIndexOf("</section>");
            if (endIdx != -1) {
                return response.substring(startIdx, endIdx + 10).trim();
            }
        }

        throw new IllegalArgumentException("No valid HTML found in response: " + response);
    }

	private void saveSlideStackIfTemporary() {
		// Only save if this is a temporary stack (starts with "temp_")
		if (stackId != null && stackId.startsWith("temp_")) {
			try {
				// Change the ID to a permanent one
				stackId = "stack_" + System.currentTimeMillis();

				// Get current slide data from code fragment
				ensureFragmentReferences();
				if (codeFragment != null) {
					List<String> allSlides = codeFragment.getAllSlides();
					if (!allSlides.isEmpty()) {
						// Save to shared preferences
						android.content.SharedPreferences sharedPreferences = getSharedPreferences("slide_stacks", MODE_PRIVATE);
						String stacksJson = sharedPreferences.getString("stacks", "[]");

						JSONArray stacksArray = new JSONArray(stacksJson);

						// Create new stack object
						JSONObject stackObj = new JSONObject();
						stackObj.put("id", stackId);
						stackObj.put("name", stackName);
						stackObj.put("createdAt", System.currentTimeMillis());

						// Add slides array
						JSONArray slidesArray = new JSONArray();
						for (String slide : allSlides) {
							slidesArray.put(slide);
						}
						stackObj.put("slides", slidesArray);

						// Add to stacks array
						stacksArray.put(stackObj);

						// Save back to preferences
						sharedPreferences.edit()
							.putString("stacks", stacksArray.toString())
							.apply();
					}
				}
			} catch (JSONException e) {
				Log.e("SlideActivity", "Error saving slide stack: " + e.getMessage());
			}
		}
	}

	@Override
	public boolean onSupportNavigateUp() {
		finish();
		return true;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (networkManager != null) {
			networkManager.cleanup();
		}
		if (executorService != null) {
			executorService.shutdown();
		}
	}

	private int dpToPx(float dp) {
		return (int) (dp * getResources().getDisplayMetrics().density);
	}

	private void showMessage(String message) {
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}

	public HashMap<String, Bitmap> getImageCache() {
		return imageCache;
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_slide, menu);
        return true;
    }

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if (item.getItemId() == R.id.action_download) {
            Toast.makeText(this, "Exporting is not supported in this version.", Toast.LENGTH_SHORT).show();
			return true;
		}
        if (item.getItemId() == R.id.action_more) {
            // Handle more options
            Toast.makeText(this, "More options coming soon!", Toast.LENGTH_SHORT).show();
            return true;
        }
		return super.onOptionsItemSelected(item);
	}
}
