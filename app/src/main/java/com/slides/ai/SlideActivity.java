package com.slides.ai;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.Manifest;
import android.widget.LinearLayout;
import android.app.Activity;
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
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.IOException;

public class SlideActivity extends Activity implements SlideRenderer.ElementSelectionListener,
CustomizationManager.ImageSelectionCallback {

	private MaterialCardView slideContainer;
	private TextInputEditText userPrompt;
	private FloatingActionButton icSend;
	private View downloadImage;
	private View importIc;
	private CustomView slideView;

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
	private CustomizationManager customizationManager;
	private ApiKeyManager apiKeyManager;

	private static final int SLIDE_WIDTH = 320;
	private static final int SLIDE_HEIGHT = 200;
	private static final int PICK_IMAGE_REQUEST = 1;
	private SlideElement selectedElement;
	
	// Stack information for saving
	private String stackId;
	private String stackName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		// Get stack information from intent
		Intent intent = getIntent();
		stackId = intent.getStringExtra("stack_id");
		stackName = intent.getStringExtra("stack_name");

		slideContainer = findViewById(R.id.slide);
		userPrompt = findViewById(R.id.userPrompt);
		icSend = findViewById(R.id.icSend);
		downloadImage = findViewById(R.id.downloadImage);
		importIc = findViewById(R.id.importIc); // Ensure this ID is in your layout

		mainHandler = new Handler();
		executorService = Executors.newCachedThreadPool();

		slideView = new CustomView(this);
		slideContainer.addView(slideView);

		slideRenderer = new SlideRenderer(this, slideView, imageCache);
		slideRenderer.setElementSelectionListener(this);

		apiKeyManager = new ApiKeyManager(this);
		networkManager = new NetworkManager(apiKeyManager, imageCache, mainHandler, executorService);

		customizationManager = new CustomizationManager(this, slideRenderer);
		customizationManager.setImageSelectionCallback(this);

		icSend.setOnClickListener(v -> {
			String prompt = userPrompt.getText().toString().trim();
			
			// Check if prompt is empty
			if (prompt.isEmpty()) {
				showMessage("Please enter a prompt");
				return;
			}
			
			// Check internet connectivity
			if (!SketchwareUtil.isConnected(this)) {
				showMessage("No internet connection available");
				return;
			}
			
			// Check if API key is set
			String apiKey = apiKeyManager.getActiveApiKey();
			if (apiKey == null) {
				showApiKeyDialog(prompt);
				return;
			}
			
			// All checks passed, send the prompt
			sendPromptToGemini(prompt);
			userPrompt.setText("");
		});

		downloadImage.setOnClickListener(v -> showDownloadOptionsDialog());

		importIc.setOnClickListener(v -> showJsonInputDialog());
	}

	private void showApiKeyDialog(String pendingPrompt) {
		LayoutInflater inflater = LayoutInflater.from(this);
		View dialogView = inflater.inflate(R.layout.dialog_add_api_key, null);
		
		TextInputEditText etApiKey = dialogView.findViewById(R.id.keyEdit);
		
		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
		builder.setTitle("API Key Required")
			.setMessage("Please enter your Gemini API key to continue")
			.setView(dialogView)
			.setPositiveButton("Save", (dialog, which) -> {
				String apiKey = etApiKey.getText().toString().trim();
				if (!apiKey.isEmpty()) {
					apiKeyManager.addApiKey("Gemini API Key", apiKey);
					sendPromptToGemini(pendingPrompt);
					userPrompt.setText("");
					showMessage("API key saved successfully");
				} else {
					showMessage("Please enter a valid API key");
				}
			})
			.setNegativeButton("Cancel", (dialog, which) -> {
				dialog.dismiss();
			})
			.setCancelable(false)
			.show();
	}

	private void sendPromptToGemini(String prompt) {
		showMessage("Generating slide...");

		networkManager.sendPromptToGemini(prompt, new NetworkManager.ApiResponseCallback() {
			@Override
			public void onSuccess(String jsonStr) {
				processSlideJSON(jsonStr);
			}

			@Override
			public void onError(String errorMessage) {
				showMessage(errorMessage);
			}
		});
	}

	private void processSlideJSON(String jsonStr) {
		try {
			JSONObject json = new JSONObject(jsonStr);
			slideRenderer.setSlideData(json);

			JSONArray elements = json.getJSONArray("elements");
			for (int i = 0; i < elements.length(); i++) {
				JSONObject element = elements.getJSONObject(i);
				if (element.getString("type").equalsIgnoreCase("image")) {
					String url = element.getString("url");
					loadImage(url);
				}
			}

			showMessage("Slide created successfully!");
			
			// Save the stack now that it has content
			saveSlideStackIfTemporary();

		} catch (JSONException e) {
			showMessage("Error parsing JSON: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void loadImage(String url) {
		networkManager.loadImage(url, new NetworkManager.ImageLoadCallback() {
			@Override
			public void onImageLoaded() {
				slideView.invalidate();
			}

			@Override
			public void onImageLoadFailed(String errorMessage) {
				showMessage("Failed to load image: " + errorMessage);
			}
		});
	}

	private void saveSlideStackIfTemporary() {
		// Only save if this is a temporary stack (starts with "temp_")
		if (stackId != null && stackId.startsWith("temp_")) {
			try {
				// Change the ID to a permanent one
				stackId = "stack_" + System.currentTimeMillis();
				
				// Get current slide data
				JSONObject slideData = slideRenderer.getSlideData();
				if (slideData != null) {
					// Create a slide stack with the current slide
					List<JSONObject> slides = new ArrayList<>();
					slides.add(slideData);
					
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
					for (JSONObject slide : slides) {
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
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

	private void saveSlideAsImage(String format, int qualityValue, float scale, boolean transparent) {
		showMessage("Exporting slide...");

		// Create bitmap with appropriate scale
		Bitmap bitmap = Bitmap.createBitmap(
		(int) (slideView.getWidth() * scale),
		(int) (slideView.getHeight() * scale),
		transparent ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565
		);

		Canvas canvas = new Canvas(bitmap);
		if (!transparent) {
			canvas.drawColor(Color.WHITE);
		}

		// Scale the canvas before drawing
		canvas.scale(scale, scale);
		slideView.draw(canvas);

		Bitmap.CompressFormat compressFormat = format.equals("PNG") ?
		Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			saveImageAPI29AndAbove(bitmap, compressFormat, qualityValue);
		} else {
			saveImageLegacy(bitmap, compressFormat, qualityValue);
		}
	}

	private void saveImageAPI29AndAbove(Bitmap bitmap, Bitmap.CompressFormat format, int quality) {
		ContentResolver resolver = getContentResolver();
		ContentValues contentValues = new ContentValues();

		String extension = format == Bitmap.CompressFormat.PNG ? "png" : "jpg";
		String fileName = "Slide_" + System.currentTimeMillis() + "." + extension;

		contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
		contentValues.put(MediaStore.MediaColumns.MIME_TYPE,
		format == Bitmap.CompressFormat.PNG ? "image/png" : "image/jpeg");
		contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH,
		Environment.DIRECTORY_PICTURES + "/SlideS/Images");

		try {
			Uri imageUri = resolver.insert(
			MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
			contentValues
			);
			if (imageUri != null) {
				OutputStream outputStream = resolver.openOutputStream(imageUri);
				if (outputStream != null) {
					bitmap.compress(format, quality, outputStream);
					outputStream.close();
					showMessage("Image saved to Pictures/SlideS/Images");
				}
			}
		} catch (Exception e) {
			showMessage("Error saving image: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void saveImageLegacy(Bitmap bitmap, Bitmap.CompressFormat format, int quality) {
		File directory = new File(
		Environment.getExternalStoragePublicDirectory(
		Environment.DIRECTORY_PICTURES
		),
		"/SlideS/Images"
		);
		if (!directory.exists()) {
			directory.mkdirs();
		}

		String extension = format == Bitmap.CompressFormat.PNG ? "png" : "jpg";
		String fileName = "Slide_" + System.currentTimeMillis() + "." + extension;
		File file = new File(directory, fileName);

		try {
			FileOutputStream fos = new FileOutputStream(file);
			bitmap.compress(format, quality, fos);
			fos.flush();
			fos.close();

			MediaScannerConnection.scanFile(
			this,
			new String[]{file.getAbsolutePath()},
			new String[]{format == Bitmap.CompressFormat.PNG ? "image/png" : "image/jpeg"},
			null
			);

			showMessage("Image saved to Pictures/SlideS/Images");
		} catch (Exception e) {
			showMessage("Error saving image: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void saveAsPdf() {
		if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
			if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
			!= PackageManager.PERMISSION_GRANTED) {
				requestPermissions(
				new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
				1002
				);
				return;
			}
		}

		showMessage("Creating PDF...");

		// Create a new PDF document
		PdfDocument document = new PdfDocument();

		// Create a page description (A4 size at 72dpi)
		int pageWidth = 595;  // A4 width in points (1pt = 1/72 inch)
		int pageHeight = 842; // A4 height in points

		// Calculate scale to fit slide content while maintaining aspect ratio
		float scale = Math.min((float)pageWidth / slideView.getWidth(),
		(float)pageHeight / slideView.getHeight());

		PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
		pageWidth, pageHeight, 1).create();

		// Start a page
		PdfDocument.Page page = document.startPage(pageInfo);

		// Draw the slide content on the PDF page's canvas
		Canvas canvas = page.getCanvas();
		canvas.drawColor(Color.WHITE); // Set white background for PDF

		// Scale and center the slide content
		canvas.save();
		canvas.scale(scale, scale);
		canvas.translate(
		(pageWidth/scale - slideView.getWidth())/2,
		(pageHeight/scale - slideView.getHeight())/2
		);
		slideView.draw(canvas);
		canvas.restore();

		// Finish the page
		document.finishPage(page);

		// Save the PDF file
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			savePdfAPI29AndAbove(document);
		} else {
			savePdfLegacy(document);
		}

		// Close the document
		document.close();
	}

	private void savePdfAPI29AndAbove(PdfDocument document) {
		ContentResolver resolver = getContentResolver();
		ContentValues contentValues = new ContentValues();

		String fileName = "Slide_" + System.currentTimeMillis() + ".pdf";
		contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
		contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
		contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH,
		Environment.DIRECTORY_DOCUMENTS + "/SlideS/PDFs");

		try {
			Uri pdfUri = resolver.insert(
			MediaStore.Files.getContentUri("external"),
			contentValues
			);

			if (pdfUri != null) {
				OutputStream outputStream = resolver.openOutputStream(pdfUri);
				if (outputStream != null) {
					document.writeTo(outputStream);
					outputStream.close();
					showMessage("PDF saved to Documents/SlideS/PDFs");
				}
			}
		} catch (Exception e) {
			showMessage("Error saving PDF: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void savePdfLegacy(PdfDocument document) {
		File directory = new File(
		Environment.getExternalStoragePublicDirectory(
		Environment.DIRECTORY_DOCUMENTS
		),
		"SlideS/PDFs"
		);
		if (!directory.exists()) {
			directory.mkdirs();
		}

		String fileName = "Slide_" + System.currentTimeMillis() + ".pdf";
		File file = new File(directory, fileName);

		try {
			FileOutputStream fos = new FileOutputStream(file);
			document.writeTo(fos);
			fos.flush();
			fos.close();

			MediaScannerConnection.scanFile(
			this,
			new String[]{file.getAbsolutePath()},
			new String[]{"application/pdf"},
			null
			);

			showMessage("PDF saved to Documents/SlideS/PDFs");
		} catch (Exception e) {
			showMessage("Error saving PDF: " + e.getMessage());
			e.printStackTrace();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
			Uri imageUri = data.getData();
			try {
				InputStream inputStream = getContentResolver().openInputStream(imageUri);
				Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
				if (bitmap != null && selectedElement != null && selectedElement instanceof ImageElement) {
					String imageKey = "custom_image_" + System.currentTimeMillis();
					imageCache.put(imageKey, bitmap);
					((ImageElement) selectedElement).setCustomImage(imageKey);
					slideView.invalidate();
				}
			} catch (Exception e) {
				showMessage("Error loading image: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		if (requestCode == 1001) { // For image saving
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				if (pendingExportFormat != null) {
					if (pendingExportFormat.equals("PDF")) {
						saveAsPdf();
					} else {
						saveSlideAsImage(pendingExportFormat, pendingExportQuality, pendingExportScale, pendingExportTransparent);
					}
				}
			} else {
				showMessage("Storage permission is required to save images");
			}
		} else if (requestCode == 1002) { // For PDF saving
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				saveAsPdf();
			} else {
				showMessage("Storage permission is required to save PDFs");
			}
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		networkManager.cleanup();
		executorService.shutdown();
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
		customizationManager.showElementCustomizationDialog(element);
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
				saveAsPdf();
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

				saveSlideAsImage(format, qualityValue, scale, transparent);
			}
		})
		.setNegativeButton("Cancel", null);

		AlertDialog dialog = builder.create();
		dialog.show();

		dialog.getWindow().setLayout(
		ViewGroup.LayoutParams.MATCH_PARENT,
		ViewGroup.LayoutParams.WRAP_CONTENT
		);
	}

	private Bitmap createSlideBitmap() {
		Bitmap bitmap = Bitmap.createBitmap(
		slideView.getWidth(),
		slideView.getHeight(),
		Bitmap.Config.ARGB_8888
		);
		Canvas canvas = new Canvas(bitmap);
		canvas.drawColor(Color.WHITE);
		slideView.draw(canvas);
		return bitmap;
	}

	private void showJsonInputDialog() {
		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this,
		R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered);

		View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_json_input, null);
		TextInputEditText etJson = dialogView.findViewById(R.id.et_json_input);

		builder.setTitle("Import Slide JSON")
		.setView(dialogView)
		.setPositiveButton("Render", (dialog, which) -> {
			String jsonStr = etJson.getText().toString().trim();
			if (!jsonStr.isEmpty()) {
				processSlideJSON(jsonStr);
			} else {
				showMessage("Please enter valid JSON");
			}
		})
		.setNegativeButton("Cancel", null);

		AlertDialog dialog = builder.create();
		dialog.show();

		etJson.post(() -> {
			Window window = dialog.getWindow();
			if (window != null) {
				window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
				(int) (getResources().getDisplayMetrics().heightPixels * 0.7));
			}
		});
	}

	private class CustomView extends View {
		public CustomView(Context context) {
			super(context);
			setClickable(true);
			setFocusable(true);
		}

		@Override
		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);
			slideRenderer.draw(canvas);
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			return slideRenderer.handleTouchEvent(event);
		}
	}
}
