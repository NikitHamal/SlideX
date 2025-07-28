package com.slides.ai;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
* CustomizationManager handles all dialog-related operations for customizing slide elements
*/
public class CustomizationManager {
	private Context context;
	private SlideRenderer slideRenderer;
	
	// Interface for color selection callback
	public interface ColorSelectedListener {
		void onColorSelected(int color);
	}
	
	// Interface for image selection callback
	public interface ImageSelectionCallback {
		void onImageSelectionRequested(SlideElement element);
	}
	
	private ImageSelectionCallback imageSelectionCallback;
	
	public CustomizationManager(Context context, SlideRenderer slideRenderer) {
		this.context = context;
		this.slideRenderer = slideRenderer;
	}
	
	public void setImageSelectionCallback(ImageSelectionCallback callback) {
		this.imageSelectionCallback = callback;
	}
	
	public void showElementCustomizationDialog(SlideElement element) {
		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context, 
		R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered);
		
		builder.setTitle("Customize Element")
		.setView(createCustomizationView(element))
		.setPositiveButton("Close", null)
		.setNegativeButton("Copy JSON", (dialog, which) -> copyElementToClipboard(element))
		.show();
	}
	
	private View createCustomizationView(SlideElement element) {
		View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_customize, null);
		
		// Hide all sections first
		setAllSectionsVisibility(dialogView, View.GONE);
		
		if (element instanceof TextElement) {
			setupTextCustomization(dialogView, (TextElement) element);
		} else if (element instanceof ImageElement) {
			setupImageCustomization(dialogView, (ImageElement) element);
		} else if (element instanceof ShapeElement) {
			setupShapeCustomization(dialogView, (ShapeElement) element);
		} else if (element instanceof TableElement) {
			setupTableCustomization(dialogView, (TableElement) element);
		} else if (element instanceof ChartElement) {
			setupChartCustomization(dialogView, (ChartElement) element);
		} else if (element instanceof IconElement) {
			setupIconCustomization(dialogView, (IconElement) element);
		}

		setupArrangeSection(dialogView, element);
		
		return dialogView;
	}
	
	private void setAllSectionsVisibility(View dialogView, int visibility) {
		int[] sectionIds = {
			R.id.text_customization, R.id.image_customization,
			R.id.shape_customization, R.id.table_customization,
			R.id.chart_customization, R.id.icon_customization
		};
		
		for (int id : sectionIds) {
			View section = dialogView.findViewById(id);
			if (section != null) {
				section.setVisibility(visibility);
			}
		}
	}
	
	// ================== TEXT ELEMENT ==================
	private void setupTextCustomization(View dialogView, TextElement textElement) {
		View section = dialogView.findViewById(R.id.text_customization);
		section.setVisibility(View.VISIBLE);
		
		TextInputEditText contentEdit = dialogView.findViewById(R.id.edit_content);
		TextInputEditText fontSizeEdit = dialogView.findViewById(R.id.edit_font_size);
		MaterialButton colorButton = dialogView.findViewById(R.id.btn_color);
		ChipGroup fontStyleGroup = dialogView.findViewById(R.id.radio_font_style);
		ChipGroup alignmentGroup = dialogView.findViewById(R.id.radio_alignment);
		
		// Set initial values
		contentEdit.setText(textElement.content);
		fontSizeEdit.setText(String.valueOf((int)textElement.fontSize));
		colorButton.setBackgroundTintList(ColorStateList.valueOf(textElement.color));
		
		// Set font style
		if (textElement.bold) {
			fontStyleGroup.check(R.id.radio_bold);
		} else if (textElement.medium) {
			fontStyleGroup.check(R.id.radio_medium);
		} else {
			fontStyleGroup.check(R.id.radio_regular);
		}
		
		// Set alignment
		if (textElement.alignment.equalsIgnoreCase("center")) {
			alignmentGroup.check(R.id.radio_center);
		} else if (textElement.alignment.equalsIgnoreCase("right")) {
			alignmentGroup.check(R.id.radio_right);
		} else {
			alignmentGroup.check(R.id.radio_left);
		}
		
		// Set up listeners
		contentEdit.addTextChangedListener(new SimpleTextWatcher() {
			public void afterTextChanged(Editable s) {
				textElement.content = s.toString();
				textElement.createTextLayout();
				slideRenderer.slideView.invalidate();
			}
		});
		
		fontSizeEdit.addTextChangedListener(new SimpleTextWatcher() {
			public void afterTextChanged(Editable s) {
				try {
					textElement.fontSize = Float.parseFloat(s.toString());
					textElement.createTextLayout();
					slideRenderer.slideView.invalidate();
				} catch (NumberFormatException ignored) {}
			}
		});
		
		colorButton.setOnClickListener(v -> showColorPickerDialog(color -> {
			textElement.color = color;
			colorButton.setBackgroundTintList(ColorStateList.valueOf(color));
			textElement.createTextLayout();
			slideRenderer.slideView.invalidate();
		}));
		
		fontStyleGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
			textElement.bold = checkedIds.contains(R.id.radio_bold);
			textElement.medium = checkedIds.contains(R.id.radio_medium);
			textElement.createTextLayout();
			slideRenderer.slideView.invalidate();
		});
		
		alignmentGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
			if (checkedIds.contains(R.id.radio_center)) {
				textElement.alignment = "center";
			} else if (checkedIds.contains(R.id.radio_right)) {
				textElement.alignment = "right";
			} else {
				textElement.alignment = "left";
			}
			textElement.createTextLayout();
			slideRenderer.slideView.invalidate();
		});
	}
	
	// ================== IMAGE ELEMENT ==================
	private void setupImageCustomization(View dialogView, ImageElement imageElement) {
		View section = dialogView.findViewById(R.id.image_customization);
		section.setVisibility(View.VISIBLE);
		
		MaterialButton pickImageButton = dialogView.findViewById(R.id.btn_pick_image);
		TextInputEditText urlEdit = dialogView.findViewById(R.id.edit_image_url);
		Slider cornerRadiusSlider = dialogView.findViewById(R.id.seekbar_corner_radius);
		
		// Set initial values
		urlEdit.setText(imageElement.url);
		cornerRadiusSlider.setValue((int)(imageElement.cornerRadius / dpToPx(1)));
		
		pickImageButton.setOnClickListener(v -> {
			if (imageSelectionCallback != null) {
				imageSelectionCallback.onImageSelectionRequested(imageElement);
			}
		});
		
		urlEdit.addTextChangedListener(new SimpleTextWatcher() {
			public void afterTextChanged(Editable s) {
				String newUrl = s.toString();
				if (!newUrl.equals(imageElement.url)) {
					imageElement.url = newUrl;
					if (imageSelectionCallback != null) {
						imageSelectionCallback.onImageSelectionRequested(imageElement);
					}
					slideRenderer.slideView.invalidate();
				}
			}
		});
		
		cornerRadiusSlider.addOnChangeListener((slider, value, fromUser) -> {
			imageElement.cornerRadius = dpToPx(value);
			imageElement.updatePath();
			slideRenderer.slideView.invalidate();
		});

		SwitchMaterial lockAspectRatioSwitch = dialogView.findViewById(R.id.switch_lock_aspect_ratio);
		lockAspectRatioSwitch.setChecked(imageElement.lockAspectRatio);
		lockAspectRatioSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
			imageElement.lockAspectRatio = isChecked;
		});
	}
	
	// ================== SHAPE ELEMENT ==================
	private void setupShapeCustomization(View dialogView, ShapeElement shapeElement) {
		View section = dialogView.findViewById(R.id.shape_customization);
		section.setVisibility(View.VISIBLE);
		
		TextInputLayout shapeTypeLayout = dialogView.findViewById(R.id.spinner_shape_type);
		AutoCompleteTextView shapeTypeSpinner = shapeTypeLayout.findViewById(R.id.edit_shape_type);
		MaterialButton colorButton = dialogView.findViewById(R.id.btn_shape_color);
		MaterialButton strokeColorButton = dialogView.findViewById(R.id.btn_stroke_color);
		Slider opacitySlider = dialogView.findViewById(R.id.seekbar_opacity);
		Slider cornerRadiusSlider = dialogView.findViewById(R.id.seekbar_corner_radius);
		Slider strokeWidthSlider = dialogView.findViewById(R.id.seekbar_stroke_width);
		
		// Setup shape type spinner
		String[] shapeTypes = new String[]{"Rectangle", "Oval", "Line", "Triangle", "Star", "Hexagon"};
		ArrayAdapter<String> adapter = new ArrayAdapter<>(context, 
		android.R.layout.simple_dropdown_item_1line, shapeTypes);
		shapeTypeSpinner.setAdapter(adapter);
		
		// Set current shape type
		int shapeTypePosition = 0;
		switch (shapeElement.shapeType.toLowerCase()) {
			case "rectangle": shapeTypePosition = 0; break;
			case "oval": shapeTypePosition = 1; break;
			case "line": shapeTypePosition = 2; break;
			case "triangle": shapeTypePosition = 3; break;
			case "star": shapeTypePosition = 4; break;
			case "hexagon": shapeTypePosition = 5; break;
		}
		shapeTypeSpinner.setText(shapeTypes[shapeTypePosition], false);
		
		// Set initial values
		colorButton.setBackgroundTintList(ColorStateList.valueOf(shapeElement.color));
		strokeColorButton.setBackgroundTintList(ColorStateList.valueOf(shapeElement.strokeColor));
		opacitySlider.setValue((int)(shapeElement.opacity * 100));
		cornerRadiusSlider.setValue((int)(shapeElement.cornerRadius / dpToPx(1)));
		strokeWidthSlider.setValue((int)(shapeElement.strokeWidth / dpToPx(1)));
		
		// Set up listeners
		shapeTypeSpinner.setOnItemClickListener((parent, view, position, id) -> {
			switch (position) {
				case 0: shapeElement.shapeType = "rectangle"; break;
				case 1: shapeElement.shapeType = "oval"; break;
				case 2: shapeElement.shapeType = "line"; break;
				case 3: shapeElement.shapeType = "triangle"; break;
				case 4: shapeElement.shapeType = "star"; break;
				case 5: shapeElement.shapeType = "hexagon"; break;
			}
			shapeElement.createShapePath();
			slideRenderer.slideView.invalidate();
		});
		
		colorButton.setOnClickListener(v -> showColorPickerDialog(color -> {
			shapeElement.color = color;
			colorButton.setBackgroundTintList(ColorStateList.valueOf(color));
			slideRenderer.slideView.invalidate();
		}));
		
		strokeColorButton.setOnClickListener(v -> showColorPickerDialog(color -> {
			shapeElement.strokeColor = color;
			strokeColorButton.setBackgroundTintList(ColorStateList.valueOf(color));
			slideRenderer.slideView.invalidate();
		}));
		
		opacitySlider.addOnChangeListener((slider, value, fromUser) -> {
			shapeElement.opacity = value / 100f;
			slideRenderer.slideView.invalidate();
		});
		
		cornerRadiusSlider.addOnChangeListener((slider, value, fromUser) -> {
			shapeElement.cornerRadius = dpToPx(value);
			slideRenderer.slideView.invalidate();
		});
		
		strokeWidthSlider.addOnChangeListener((slider, value, fromUser) -> {
			shapeElement.strokeWidth = dpToPx(value);
			slideRenderer.slideView.invalidate();
		});

		SwitchMaterial lockAspectRatioSwitch = dialogView.findViewById(R.id.switch_shape_lock_aspect_ratio);
		lockAspectRatioSwitch.setChecked(shapeElement.lockAspectRatio);
		lockAspectRatioSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
			shapeElement.lockAspectRatio = isChecked;
		});
	}
	
	// ================== TABLE ELEMENT ==================
	private void setupTableCustomization(View dialogView, TableElement tableElement) {
		View section = dialogView.findViewById(R.id.table_customization);
		section.setVisibility(View.VISIBLE);
		
		MaterialButton headerColorButton = dialogView.findViewById(R.id.btn_header_color);
		MaterialButton cellColorButton = dialogView.findViewById(R.id.btn_cell_color);
		MaterialButton borderColorButton = dialogView.findViewById(R.id.btn_border_color);
		Slider borderWidthSlider = dialogView.findViewById(R.id.seekbar_border_width);
		
		// Set initial values
		headerColorButton.setBackgroundTintList(ColorStateList.valueOf(tableElement.headerColor));
		cellColorButton.setBackgroundTintList(ColorStateList.valueOf(tableElement.cellColor));
		borderColorButton.setBackgroundTintList(ColorStateList.valueOf(tableElement.borderColor));
		borderWidthSlider.setValue((int)(tableElement.borderWidth / dpToPx(1)));
		
		// Set up listeners
		headerColorButton.setOnClickListener(v -> showColorPickerDialog(color -> {
			tableElement.headerColor = color;
			headerColorButton.setBackgroundTintList(ColorStateList.valueOf(color));
			tableElement.initializePaints();
			slideRenderer.slideView.invalidate();
		}));
		
		cellColorButton.setOnClickListener(v -> showColorPickerDialog(color -> {
			tableElement.cellColor = color;
			cellColorButton.setBackgroundTintList(ColorStateList.valueOf(color));
			tableElement.initializePaints();
			slideRenderer.slideView.invalidate();
		}));
		
		borderColorButton.setOnClickListener(v -> showColorPickerDialog(color -> {
			tableElement.borderColor = color;
			borderColorButton.setBackgroundTintList(ColorStateList.valueOf(color));
			tableElement.initializePaints();
			slideRenderer.slideView.invalidate();
		}));
		
		borderWidthSlider.addOnChangeListener((slider, value, fromUser) -> {
			tableElement.borderWidth = dpToPx(value);
			tableElement.initializePaints();
			slideRenderer.slideView.invalidate();
		});
	}
	// ================== CHART ELEMENT ==================
	private void setupChartCustomization(View dialogView, ChartElement chartElement) {
		View section = dialogView.findViewById(R.id.chart_customization);
		section.setVisibility(View.VISIBLE);
		
		ChipGroup chartTypeGroup = dialogView.findViewById(R.id.chart_type_group);
		TextInputEditText chartDataEdit = dialogView.findViewById(R.id.edit_chart_data);
		
		// Set initial values
		switch (chartElement.getChartType()) {
			case "bar": chartTypeGroup.check(R.id.chip_bar); break;
			case "pie": chartTypeGroup.check(R.id.chip_pie); break;
		}
		
		chartDataEdit.setText(chartElement.getData().toString());
		
		// Set up listeners
		chartTypeGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
			if (checkedIds.contains(R.id.chip_bar)) {
				chartElement.setChartType("bar");
			} else if (checkedIds.contains(R.id.chip_pie)) {
				chartElement.setChartType("pie");
			}
			slideRenderer.slideView.invalidate();
		});
		
		chartDataEdit.addTextChangedListener(new SimpleTextWatcher() {
			public void afterTextChanged(Editable s) {
				try {
					chartElement.setData(new JSONArray(s.toString()));
					slideRenderer.slideView.invalidate();
				} catch (JSONException e) {
					Toast.makeText(context, "Invalid JSON", Toast.LENGTH_SHORT).show();
				}
			}
		});
	}
	
	// ================== ICON ELEMENT ==================
	private void setupIconCustomization(View dialogView, IconElement iconElement) {
		View section = dialogView.findViewById(R.id.icon_customization);
		section.setVisibility(View.VISIBLE);

		AutoCompleteTextView iconSelector = dialogView.findViewById(R.id.icon_selector);
		MaterialButton colorButton = dialogView.findViewById(R.id.btn_icon_color);

		// Setup icon dropdown
		String[] icons = {"home", "settings", "pie_chart", "bar_chart", "image", "text_fields"};
		ArrayAdapter<String> adapter = new ArrayAdapter<>(
				context, android.R.layout.simple_dropdown_item_1line, icons);
		iconSelector.setAdapter(adapter);
		iconSelector.setText(iconElement.getIconName(), false);

		// Set initial color
		colorButton.setBackgroundTintList(ColorStateList.valueOf(iconElement.getColor()));

		// Set up listeners
		iconSelector.setOnItemClickListener((parent, view, position, id) -> {
			iconElement.setIconName(icons[position]);
			slideRenderer.slideView.invalidate();
		});

		colorButton.setOnClickListener(v -> showColorPickerDialog(color -> {
			iconElement.setColor(color);
			colorButton.setBackgroundTintList(ColorStateList.valueOf(color));
			slideRenderer.slideView.invalidate();
		}));
	}

	private void setupArrangeSection(View dialogView, SlideElement element) {
		View section = dialogView.findViewById(R.id.arrange_section);
		section.setVisibility(View.VISIBLE);

		Slider rotationSlider = dialogView.findViewById(R.id.slider_rotation);
		MaterialButton bringToFrontButton = dialogView.findViewById(R.id.btn_bring_to_front);
		MaterialButton sendToBackButton = dialogView.findViewById(R.id.btn_send_to_back);

		rotationSlider.setValue(element.rotation);

		rotationSlider.addOnChangeListener((slider, value, fromUser) -> {
			element.rotation = value;
			slideRenderer.slideView.invalidate();
		});

		bringToFrontButton.setOnClickListener(v -> {
			slideRenderer.bringToFront(element);
			slideRenderer.slideView.invalidate();
		});

		sendToBackButton.setOnClickListener(v -> {
			slideRenderer.sendToBack(element);
			slideRenderer.slideView.invalidate();
		});
	}
	
	private void copyElementToClipboard(SlideElement element) {
		try {
			ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
			JSONObject json = element.toJson(); // Get the JSON object
			ClipData clip = ClipData.newPlainText("Slide Element", json.toString());
			clipboard.setPrimaryClip(clip);
			Toast.makeText(context, "Element JSON copied", Toast.LENGTH_SHORT).show();
		} catch (JSONException e) {
			Toast.makeText(context, "Error copying element", Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}
	}
	
	
	public void showColorPickerDialog(final ColorSelectedListener listener) {
		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
		builder.setTitle("Select Color");
		
		// Create a grid of color options
		View colorPickerView = LayoutInflater.from(context).inflate(R.layout.color_picker_material3, null);
		builder.setView(colorPickerView);
		
		// Get color grid container
		ViewGroup colorGrid = colorPickerView.findViewById(R.id.color_grid);
		
		// Define Material 3 color palette
		int[][] colorRows = {
			{
				ContextCompat.getColor(context, R.color.md_theme_primary),
				ContextCompat.getColor(context, R.color.md_theme_primaryContainer),
				ContextCompat.getColor(context, R.color.md_theme_secondary),
				ContextCompat.getColor(context, R.color.md_theme_secondaryContainer)
			},
			{
				ContextCompat.getColor(context, R.color.md_theme_tertiary),
				ContextCompat.getColor(context, R.color.md_theme_tertiaryContainer),
				ContextCompat.getColor(context, R.color.md_theme_error),
				ContextCompat.getColor(context, R.color.md_theme_errorContainer)
			},
			{
				Color.BLACK,
				Color.DKGRAY,
				Color.GRAY,
				Color.LTGRAY,
				Color.WHITE
			},
			{
				Color.RED,
				Color.parseColor("#FF4500"),
				Color.parseColor("#FFA500"),
				Color.YELLOW
			},
			{
				Color.parseColor("#00FF00"),
				Color.parseColor("#008000"),
				Color.parseColor("#00FFFF"),
				Color.parseColor("#0000FF")
			}
		};
		
		// Create color buttons
		for (int[] row : colorRows) {
			LinearLayout rowLayout = new LinearLayout(context);
			rowLayout.setOrientation(LinearLayout.HORIZONTAL);
			rowLayout.setLayoutParams(new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT,
			ViewGroup.LayoutParams.WRAP_CONTENT
			));
			
			for (final int color : row) {
				MaterialButton colorButton = new MaterialButton(context);
				LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				0,
				dpToPx(48),
				1.0f
				);
				params.setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
				colorButton.setLayoutParams(params);
				colorButton.setBackgroundTintList(ColorStateList.valueOf(color));
				colorButton.setCornerRadius(dpToPx(8));
				
				colorButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						listener.onColorSelected(color);
						builder.create().dismiss();
					}
				});
				
				rowLayout.addView(colorButton);
			}
			
			colorGrid.addView(rowLayout);
		}
		
		// Get custom color input
		TextInputEditText hexInput = colorPickerView.findViewById(R.id.hex_input);
		MaterialButton applyButton = colorPickerView.findViewById(R.id.apply_button);
		
		applyButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					String hexValue = hexInput.getText().toString();
					if (!hexValue.startsWith("#")) {
						hexValue = "#" + hexValue;
					}
					int color = Color.parseColor(hexValue);
					listener.onColorSelected(color);
					builder.create().dismiss();
				} catch (IllegalArgumentException e) {
					Toast.makeText(context, "Invalid color format", Toast.LENGTH_SHORT).show();
				}
			}
		});
		
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		
		builder.show();
	}
	
	private int dpToPx(float dp) {
		return (int) (dp * context.getResources().getDisplayMetrics().density);
	}
	
	private abstract class SimpleTextWatcher implements TextWatcher {
		@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
		@Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
	}
}
