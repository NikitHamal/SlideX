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
import android.os.Looper;
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
import com.slides.ai.qwen.auth.QwenAuth;
import com.slides.ai.qwen.auth.TokenManager;

public class SlideActivity extends AppCompatActivity implements SlideRenderer.ElementSelectionListener,
CustomizationManager.ImageSelectionCallback, CodeFragment.CodeInteractionListener,
SlidesFragment.SlideNavigationListener, ChatFragment.ChatInteractionListener, SlideRenderer.ElementUpdateListener {

	private ViewPager2 viewPager;
	private TabLayout tabLayout;
	private ViewPagerAdapter adapter;

	private final String API_KEY = "Gemini_API";
	private final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";
	private HashMap<String, Bitmap> imageCache = new HashMap<>();
	private Handler mainHandler;
	private ExecutorService executorService;

	private String pendingExportFormat;
	private int pendingExportQuality;
	private float pendingExportScale;
	private boolean pendingExportTransparent;

	private SlideRenderer slideRenderer;
	private NetworkManager networkManager;
	private QwenManager qwenManager;
    private QwenAuth qwenAuth;
    private TokenManager tokenManager;
	private CustomizationManager customizationManager;
	private ApiKeyManager apiKeyManager;

	private static final int SLIDE_WIDTH = 320;
	private static final int SLIDE_HEIGHT = 200;
	private static final int PICK_IMAGE_REQUEST = 1;
	private static final int WRITE_EXTERNAL_STORAGE_PERMISSION = 2;
	private SlideElement selectedElement;
	
	// Stack information for saving
	private String stackId;
	private String stackName;

	// Fragment references
	private SlidesFragment slidesFragment;
	private CodeFragment codeFragment;
	private ChatFragment chatFragment;

    private AlertDialog loginDialog;
    private Handler pollingHandler = new Handler(Looper.getMainLooper());
    private Runnable pollingRunnable;


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
        tokenManager = new TokenManager(this);
        qwenAuth = new QwenAuth();
		networkManager = new NetworkManager(apiKeyManager, imageCache, mainHandler, executorService);
		qwenManager = new QwenManager(this);

		// Initialize slide renderer once we have the slides fragment
		setupSlideRenderer();
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

	private void setupSlideRenderer() {
		// Wait for the slides fragment to be created
		new Handler().postDelayed(() -> {
			ensureFragmentReferences();
			if (slidesFragment != null) {
				slidesFragment.getSlideRenderer().setElementUpdateListener(this);
				customizationManager = new CustomizationManager(this, slidesFragment.getSlideRenderer());
				customizationManager.setImageSelectionCallback(this);
			}
		}, 500); // Increased delay to ensure fragments are ready
	}

	@Override
	public void onElementUpdated() {
        ensureFragmentReferences();
        if (slidesFragment != null && codeFragment != null) {
            JSONObject slideData = slidesFragment.getSlideRenderer().getSlideData();
            if (slideData != null) {
                codeFragment.setCode(slideData.toString());
            }
        }
	}

	@Override
	public void onCodeSaved(String jsonCode, int slideIndex) {
		try {
			JSONObject slideData = new JSONObject(jsonCode);
			
			// Update the slides fragment with new data
			ensureFragmentReferences(); // Make sure we have fragment references
			if (slidesFragment != null && codeFragment != null) {
				// Get all slides from code fragment and update slides fragment
				List<String> allSlides = codeFragment.getAllSlides();
				List<JSONObject> slideObjects = new ArrayList<>();
				for (String slide : allSlides) {
					try {
						slideObjects.add(new JSONObject(slide));
					} catch (JSONException e) {
						Log.e("SlideActivity", "Invalid JSON for slide: " + e.getMessage());
						// Skip invalid slides but continue processing
					}
				}
				if (!slideObjects.isEmpty()) {
					slidesFragment.setSlides(slideObjects);
					slidesFragment.navigateToSlide(slideIndex);
				}
			}
			
			// Save the slide stack if it's temporary
			saveSlideStackIfTemporary();
			
			// Switch to slides tab to show the rendered slide
			viewPager.setCurrentItem(0);
			
			Toast.makeText(this, "Slide " + (slideIndex + 1) + " updated successfully", Toast.LENGTH_SHORT).show();
		} catch (JSONException e) {
			Log.e("SlideActivity", "JSON parsing error: " + e.getMessage());
			Toast.makeText(this, "Invalid JSON format: " + e.getMessage(), Toast.LENGTH_SHORT).show();
		}
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
                networkManager.sendPromptToGemini(prompt, slidesFragment.getSlideRenderer().getCanvasWidth(), slidesFragment.getSlideRenderer().getCanvasHeight(), new NetworkManager.ApiResponseCallback() {
                    @Override
                    public void onSuccess(String jsonStr) {
                        handleSuccessfulResponse(jsonStr);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        handleErrorResponse(errorMessage);
                    }
                });
            } else {
                handleErrorResponse("Network manager not available.");
            }
        } else if (selectedModel.startsWith("qwen")) {
            if (qwenManager != null) {
                qwenManager.getCompletion(prompt, selectedModel, slidesFragment.getSlideRenderer().getCanvasWidth(), slidesFragment.getSlideRenderer().getCanvasHeight(), new QwenManager.QwenCallback<String>() {
                    @Override
                    public void onSuccess(String jsonResponse) {
                        try {
                            String jsonStr = extractJsonFromResponse(jsonResponse);
                            handleSuccessfulResponse(jsonStr);
                        } catch (Exception e) {
                            Log.e("SlideActivity", "Error extracting JSON from Qwen response", e);
                            handleErrorResponse("Error extracting JSON from Qwen response: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (error.contains("log in")) {
                            promptUserToLogin();
                        } else {
                            Log.e("SlideActivity", "Qwen completion error: " + error);
                            handleErrorResponse("Qwen API error: " + error);
                        }
                    }
                });
            } else {
                Log.e("SlideActivity", "QwenManager is null");
                handleErrorResponse("Qwen manager not available. Please restart the app.");
            }
        }
	}

    private void promptUserToLogin() {
        QwenAuth.PKCE pkce = qwenAuth.generatePKCEPair();

        qwenAuth.requestDeviceAuthorization(pkce, new QwenAuth.AuthCallback<QwenAuth.DeviceAuthResponse>() {
            @Override
            public void onSuccess(QwenAuth.DeviceAuthResponse response) {
                showLoginDialog(response.verificationUri, response.userCode, response.deviceCode, pkce.codeVerifier, response.interval);
            }

            @Override
            public void onError(String message) {
                handleErrorResponse("Failed to start login process: " + message);
            }
        });
    }

    private void showLoginDialog(String verificationUri, String userCode, String deviceCode, String codeVerifier, int interval) {
        if (loginDialog != null && loginDialog.isShowing()) {
            loginDialog.dismiss();
        }

        loginDialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Login to Qwen")
                .setMessage("To continue, please open the following URL in a browser and enter the code:\n\nURL: " + verificationUri + "\nCode: " + userCode)
                .setPositiveButton("Copy Code", (dialog, which) -> {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText("Qwen Code", userCode);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "Code copied to clipboard", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                     pollingHandler.removeCallbacks(pollingRunnable);
                })
                .setCancelable(false)
                .show();

        pollForToken(deviceCode, codeVerifier, interval);
    }

    private void pollForToken(String deviceCode, String codeVerifier, int interval) {
         pollingRunnable = new Runnable() {
            @Override
            public void run() {
                qwenAuth.pollDeviceToken(deviceCode, codeVerifier, new QwenAuth.AuthCallback<QwenAuth.TokenResponse>() {
                    @Override
                    public void onSuccess(QwenAuth.TokenResponse response) {
                        tokenManager.saveToken(response);
                        if (loginDialog != null && loginDialog.isShowing()) {
                            loginDialog.dismiss();
                        }
                        Toast.makeText(SlideActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String message) {
                        if ("pending".equals(message)) {
                            pollingHandler.postDelayed(this, Math.max(interval, 5) * 1000); // Ensure interval is at least 5s
                        } else {
                             if (loginDialog != null && loginDialog.isShowing()) {
                                loginDialog.dismiss();
                            }
                            handleErrorResponse("Login failed: " + message);
                        }
                    }
                });
            }
        };
        pollingHandler.postDelayed(pollingRunnable, Math.max(interval, 5) * 1000);
    }


    private void handleSuccessfulResponse(String jsonStr) {
        try {
            // Parse the JSON and add it as a new slide
            JSONObject slideData = new JSONObject(jsonStr);

            // Add to code fragment
            ensureFragmentReferences();
            if (codeFragment != null) {
                if (codeFragment.isCurrentSlideDefault()) {
                    codeFragment.setCode(jsonStr);
                    onCodeSaved(jsonStr, 0);
                } else {
                    codeFragment.addSlideFromJson(jsonStr);
                }

                // Update slides fragment
                if (slidesFragment != null) {
                    List<String> allSlides = codeFragment.getAllSlides();
                    List<JSONObject> slideObjects = new ArrayList<>();
                    for (String slide : allSlides) {
                        try {
                            slideObjects.add(new JSONObject(slide));
                        } catch (JSONException e) {
                            Log.e("SlideActivity", "Invalid JSON for slide: " + e.getMessage());
                        }
                    }
                    if (!slideObjects.isEmpty()) {
                        slidesFragment.setSlides(slideObjects);
                        slidesFragment.navigateToSlide(slideObjects.size() - 1); // Navigate to new slide
                    }
                }
            }

            // Switch to slides tab to show the result
            viewPager.setCurrentItem(0);

            // Add AI response to chat
            if (chatFragment != null) {
                chatFragment.addAiResponse("Great! I've created a slide based on your request. You can view it in the Slides tab and edit the JSON code in the Code tab if needed.");
            }

        } catch (JSONException e) {
            Log.e("SlideActivity", "Error parsing generated JSON: " + e.getMessage());
            if (chatFragment != null) {
                chatFragment.addAiResponse("I generated a slide, but there was an error parsing the JSON. Please check the Code tab and fix any formatting issues.");
            }
        }
    }

    private void handleErrorResponse(String errorMessage) {
        Log.e("SlideActivity", "Chat API error: " + errorMessage);
        if (chatFragment != null) {
            String userFriendlyMessage;
            if (errorMessage.contains("API key")) {
                userFriendlyMessage = "Please add a valid API key in Settings to use the AI chat feature.";
            } else if (errorMessage.contains("quota") || errorMessage.contains("limit")) {
                userFriendlyMessage = "API quota exceeded. Please try again later or check your API key limits.";
            } else {
                userFriendlyMessage = "Sorry, I couldn't process your request right now. Please try again or create slides manually using the Code tab.";
            }
            chatFragment.addAiResponse(userFriendlyMessage);
        }
    }

    private String extractJsonFromResponse(String response) {
        // First try to find JSON wrapped in markdown code blocks
        if (response.contains("```json")) {
            int startIdx = response.indexOf("```json") + 7;
            int endIdx = response.lastIndexOf("```");
            if (endIdx > startIdx) {
                String jsonStr = response.substring(startIdx, endIdx).trim();
                if (isValidJson(jsonStr)) {
                    return jsonStr;
                }
            }
        }

        // Try to find JSON wrapped in any code blocks
        if (response.contains("```")) {
            int startIdx = response.indexOf("```");
            int secondStart = response.indexOf('\n', startIdx);
            if (secondStart > startIdx) {
                int endIdx = response.lastIndexOf("```");
                if (endIdx > secondStart) {
                    String jsonStr = response.substring(secondStart + 1, endIdx).trim();
                    if (isValidJson(jsonStr)) {
                        return jsonStr;
                    }
                }
            }
        }

        // Find the first complete JSON object
        int startIdx = response.indexOf('{');
        if (startIdx != -1) {
            int braceCount = 0;
            int endIdx = startIdx;
            
            for (int i = startIdx; i < response.length(); i++) {
                char c = response.charAt(i);
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0) {
                        endIdx = i;
                        break;
                    }
                }
            }
            
            if (braceCount == 0 && endIdx > startIdx) {
                String jsonStr = response.substring(startIdx, endIdx + 1);
                if (isValidJson(jsonStr)) {
                    return jsonStr;
                }
            }
        }

        throw new IllegalArgumentException("No valid JSON found in response: " + response);
    }

    private boolean isValidJson(String jsonStr) {
        try {
            new org.json.JSONObject(jsonStr);
            return true;
        } catch (org.json.JSONException e) {
            return false;
        }
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
        pollingHandler.removeCallbacks(pollingRunnable);
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

	@Override
	public void onElementSelected(SlideElement element) {
		selectedElement = element;
		if (customizationManager != null) {
			customizationManager.showElementCustomizationDialog(element);
		}
	}

	@Override
	public void onImageSelectionRequested(SlideElement element) {
		selectedElement = element;

		Intent intent = new Intent();
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_GET_CONTENT);
		startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
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
			showDownloadOptionsDialog();
			return true;
		}
        if (item.getItemId() == R.id.action_more) {
            // Handle more options
            Toast.makeText(this, "More options coming soon!", Toast.LENGTH_SHORT).show();
            return true;
        }
		return super.onOptionsItemSelected(item);
	}

	private void showDownloadOptionsDialog() {
		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this,
		R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered);

		View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_download_options, null);

		// Initialize all views first
		AutoCompleteTextView formatSpinner = dialogView.findViewById(R.id.format_spinner);
		AutoCompleteTextView qualitySpinner = dialogView.findViewById(R.id.quality_spinner);
		AutoCompleteTextView sizeSpinner = dialogView.findViewById(R.id.size_spinner);
		SwitchMaterial transparentSwitch = dialogView.findViewById(R.id.transparent_switch);

		// Setup format spinner
		String[] formats = new String[]{"PNG", "JPG", "PDF"};
		ArrayAdapter<String> formatAdapter = new ArrayAdapter<>(
		this, android.R.layout.simple_dropdown_item_1line, formats);
		formatSpinner.setAdapter(formatAdapter);
		formatSpinner.setText(formats[0], false); // Default to PNG

		// Setup quality spinner (only for PNG/JPG)
		String[] qualities = new String[]{"High (100%)", "Medium (80%)", "Low (50%)"};
		ArrayAdapter<String> qualityAdapter = new ArrayAdapter<>(
		this, android.R.layout.simple_dropdown_item_1line, qualities);
		qualitySpinner.setAdapter(qualityAdapter);
		qualitySpinner.setText(qualities[0], false); // Default to High

		// Setup size spinner
		String[] sizes = new String[]{"Original", "2x", "4x"};
		ArrayAdapter<String> sizeAdapter = new ArrayAdapter<>(
		this, android.R.layout.simple_dropdown_item_1line, sizes);
		sizeSpinner.setAdapter(sizeAdapter);
		sizeSpinner.setText(sizes[0], false); // Default to Original

		// Show transparent switch by default if PNG is selected
		transparentSwitch.setVisibility(formatSpinner.getText().toString().equals("PNG") ? View.VISIBLE : View.GONE);

		// Handle format changes
		formatSpinner.setOnItemClickListener((parent, view, position, id) -> {
			String selectedFormat = formats[position];
			transparentSwitch.setVisibility(selectedFormat.equals("PNG") ? View.VISIBLE : View.GONE);
			qualitySpinner.setEnabled(!selectedFormat.equals("PDF"));
		});

		builder.setTitle("Export Slide")
		.setView(dialogView)
		.setPositiveButton("Export", (dialog, which) -> {
			String format = formatSpinner.getText().toString();

			if (format.equals("PDF")) {
				pendingExportFormat = format;
				checkStoragePermissionAndExport();
			} else {
				// Parse quality and size
				String quality = qualitySpinner.getText().toString();
				String size = sizeSpinner.getText().toString();
				boolean transparent = transparentSwitch.isChecked() && format.equals("PNG");

				int qualityValue = 100;
				if (quality.contains("80")) {
					qualityValue = 80;
				} else if (quality.contains("50")) {
					qualityValue = 50;
				}

				float scale = 1.0f;
				if (size.equals("2x")) {
					scale = 2.0f;
				} else if (size.equals("4x")) {
					scale = 4.0f;
				}

				// Store the parameters for permission callback
				pendingExportFormat = format;
				pendingExportQuality = qualityValue;
				pendingExportScale = scale;
				pendingExportTransparent = transparent;

				// Check for storage permission and export
				checkStoragePermissionAndExport();
			}
		})
		.setNegativeButton("Cancel", null);

		AlertDialog dialog = builder.create();
		dialog.show();

		// Ensure the dialog is properly sized
		if (dialog.getWindow() != null) {
			dialog.getWindow().setLayout(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
			);
		}
	}

	private void checkStoragePermissionAndExport() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			// Android 10+ - no need for WRITE_EXTERNAL_STORAGE permission
			performExport();
		} else {
			// Android 9 and below - need WRITE_EXTERNAL_STORAGE permission
			if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
				!= PackageManager.PERMISSION_GRANTED) {
				ActivityCompat.requestPermissions(this, 
					new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 
					WRITE_EXTERNAL_STORAGE_PERMISSION);
			} else {
				performExport();
			}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == WRITE_EXTERNAL_STORAGE_PERMISSION) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				performExport();
			} else {
				Toast.makeText(this, "Storage permission required to save image", Toast.LENGTH_SHORT).show();
			}
		}
	}

	private void performExport() {
		if (pendingExportFormat == null) {
			Toast.makeText(this, "Export format not set", Toast.LENGTH_SHORT).show();
			return;
		}

		if (pendingExportFormat.equals("PDF")) {
			exportToPdf();
		} else {
			exportToImage();
		}
	}

	private void exportToImage() {
		executorService.execute(() -> {
			try {
				ensureFragmentReferences();
				if (slidesFragment == null || slidesFragment.getSlideRenderer() == null) {
					mainHandler.post(() -> Toast.makeText(this, "No slide to export", Toast.LENGTH_SHORT).show());
					return;
				}

				// Create bitmap from slide
				int width = (int)(SLIDE_WIDTH * pendingExportScale);
				int height = (int)(SLIDE_HEIGHT * pendingExportScale);
				
				Bitmap bitmap = Bitmap.createBitmap(width, height, 
					pendingExportTransparent ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
				Canvas canvas = new Canvas(bitmap);

				// Clear background
				if (!pendingExportTransparent) {
					canvas.drawColor(Color.WHITE);
				}

				// Scale canvas for higher resolution
				canvas.scale(pendingExportScale, pendingExportScale);

				// Draw slide content
				slidesFragment.getSlideRenderer().draw(canvas);

				// Save to storage
				String fileName = "slide_" + System.currentTimeMillis() + 
					(pendingExportFormat.equals("PNG") ? ".png" : ".jpg");
				
				boolean saved = saveBitmapToStorage(bitmap, fileName, pendingExportFormat, pendingExportQuality);

				mainHandler.post(() -> {
					if (saved) {
						Toast.makeText(this, "Slide exported as " + fileName, Toast.LENGTH_LONG).show();
					} else {
						Toast.makeText(this, "Failed to export slide", Toast.LENGTH_SHORT).show();
					}
				});

			} catch (Exception e) {
				Log.e("SlideActivity", "Export error: " + e.getMessage());
				mainHandler.post(() -> Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
			}
		});
	}

	private boolean saveBitmapToStorage(Bitmap bitmap, String fileName, String format, int quality) {
		try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
				// Use MediaStore for Android 10+
				ContentResolver resolver = getContentResolver();
				ContentValues contentValues = new ContentValues();
				contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
				contentValues.put(MediaStore.MediaColumns.MIME_TYPE, 
					format.equals("PNG") ? "image/png" : "image/jpeg");
				contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AI Slides");

				Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
				if (imageUri != null) {
					OutputStream outputStream = resolver.openOutputStream(imageUri);
					if (outputStream != null) {
						Bitmap.CompressFormat compressFormat = format.equals("PNG") ? 
							Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG;
						bitmap.compress(compressFormat, quality, outputStream);
						outputStream.close();
						return true;
					}
				}
			} else {
				// Use external storage for Android 9 and below
				File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
				File aiSlidesDir = new File(picturesDir, "AI Slides");
				if (!aiSlidesDir.exists()) {
					aiSlidesDir.mkdirs();
				}

				File imageFile = new File(aiSlidesDir, fileName);
				FileOutputStream outputStream = new FileOutputStream(imageFile);
				
				Bitmap.CompressFormat compressFormat = format.equals("PNG") ? 
					Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG;
				bitmap.compress(compressFormat, quality, outputStream);
				outputStream.close();

				// Notify media scanner
				MediaScannerConnection.scanFile(this, new String[]{imageFile.getAbsolutePath()}, null, null);
				return true;
			}
		} catch (Exception e) {
			Log.e("SlideActivity", "Save error: " + e.getMessage());
		}
		return false;
	}

	private void exportToPdf() {
		executorService.execute(() -> {
			try {
				ensureFragmentReferences();
				if (slidesFragment == null || slidesFragment.getSlideRenderer() == null) {
					mainHandler.post(() -> Toast.makeText(this, "No slide to export", Toast.LENGTH_SHORT).show());
					return;
				}

				// Create PDF document
				PdfDocument pdfDocument = new PdfDocument();
				PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
					SLIDE_WIDTH * 2, SLIDE_HEIGHT * 2, 1).create(); // 2x scale for better quality
				PdfDocument.Page page = pdfDocument.startPage(pageInfo);

				Canvas canvas = page.getCanvas();
				canvas.scale(2.0f, 2.0f); // Scale up for better quality

				// Draw slide content
				slidesFragment.getSlideRenderer().draw(canvas);

				pdfDocument.finishPage(page);

				// Save PDF
				String fileName = "slide_" + System.currentTimeMillis() + ".pdf";
				boolean saved = savePdfToStorage(pdfDocument, fileName);

				pdfDocument.close();

				mainHandler.post(() -> {
					if (saved) {
						Toast.makeText(this, "Slide exported as " + fileName, Toast.LENGTH_LONG).show();
					} else {
						Toast.makeText(this, "Failed to export PDF", Toast.LENGTH_SHORT).show();
					}
				});

			} catch (Exception e) {
				Log.e("SlideActivity", "PDF export error: " + e.getMessage());
				mainHandler.post(() -> Toast.makeText(this, "PDF export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
			}
		});
	}

	private boolean savePdfToStorage(PdfDocument pdfDocument, String fileName) {
		try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
				// Use MediaStore for Android 10+
				ContentResolver resolver = getContentResolver();
				ContentValues contentValues = new ContentValues();
				contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
				contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
				contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/AI Slides");

				Uri pdfUri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues);
				if (pdfUri != null) {
					OutputStream outputStream = resolver.openOutputStream(pdfUri);
					if (outputStream != null) {
						pdfDocument.writeTo(outputStream);
						outputStream.close();
						return true;
					}
				}
			} else {
				// Use external storage for Android 9 and below
				File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
				File aiSlidesDir = new File(documentsDir, "AI Slides");
				if (!aiSlidesDir.exists()) {
					aiSlidesDir.mkdirs();
				}

				File pdfFile = new File(aiSlidesDir, fileName);
				FileOutputStream outputStream = new FileOutputStream(pdfFile);
				pdfDocument.writeTo(outputStream);
				outputStream.close();

				// Notify media scanner
				MediaScannerConnection.scanFile(this, new String[]{pdfFile.getAbsolutePath()}, null, null);
				return true;
			}
		} catch (Exception e) {
			Log.e("SlideActivity", "PDF save error: " + e.getMessage());
		}
		return false;
	}
}
