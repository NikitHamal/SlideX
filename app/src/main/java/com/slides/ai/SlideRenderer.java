package com.slides.ai;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import androidx.core.content.ContextCompat;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
* SlideRenderer handles all canvas drawing operations for the slide presentation
* including zoom, pan, and element selection functionality.
*/
public class SlideRenderer {
	private Context context;
	public View slideView;
	private JSONObject slideData;
	private Paint paint;
	private int backgroundColor = Color.WHITE;
	private List<SlideElement> elements = new ArrayList<>();
	private HashMap<String, Bitmap> imageCache;
	
	private float lastTouchX, lastTouchY;
	private boolean isMovingElement = false;
	private static final int HANDLE_SIZE = 10; // Reduced from 20 to 10
	private static final int HANDLE_EDGE_THRESHOLD = 20; // Reduced from 25 to 20
	private boolean isResizing = false;
	private int resizeHandleIndex = -1;
	
	// Zoom and pan variables
	private Matrix transformMatrix = new Matrix();
	private float scaleFactor = 1.0f;
	private float minScale = 0.5f;
	private float maxScale = 3.0f;
	private float translateX = 0;
	private float translateY = 0;
	
	// Touch handling
	private ScaleGestureDetector scaleGestureDetector;
	private GestureDetector gestureDetector;
	
	// Element selection and editing
	private SlideElement selectedElement = null;
	private boolean isInEditMode = false;
	
	// Alignment guides
	private boolean showAlignmentGuides = true;
	private int guideColor = Color.parseColor("#2196F3"); // Material blue
	private float snapThreshold = 10.0f; // Distance in pixels to snap to guides
	private List<Float> horizontalGuides = new ArrayList<>();
	private List<Float> verticalGuides = new ArrayList<>();
	private boolean isSnapped = false;
	private float snapBreakThreshold = 20.0f; // Distance to move before breaking snap
	
	// Callback interface for element selection
	public interface ElementSelectionListener {
		void onElementSelected(SlideElement element);
	}
	
	private ElementSelectionListener elementSelectionListener;
	
	public SlideRenderer(Context context, View slideView, HashMap<String, Bitmap> imageCache) {
		this.context = context;
		this.slideView = slideView;
		this.imageCache = imageCache;
		init();
	}
	
	private void init() {
		paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setStyle(Paint.Style.FILL);
		
		// Set background
		setBackgroundColor(ContextCompat.getColor(context, R.color.md_theme_surface));
		
		// Initialize gesture detectors
		scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
		gestureDetector = new GestureDetector(context, new GestureListener());
	}
	
	public void setElementSelectionListener(ElementSelectionListener listener) {
		this.elementSelectionListener = listener;
	}
	
	public void setSlideData(JSONObject data) {
		slideData = data;
		parseSlideData();
		resetTransformation();
		slideView.invalidate();
	}
	
	public JSONObject getSlideData() {
		return slideData;
	}
	
	public void setBackgroundColor(int color) {
		backgroundColor = color;
	}
	
	private void resetTransformation() {
		scaleFactor = 1.0f;
		translateX = 0;
		translateY = 0;
		transformMatrix.reset();
		slideView.invalidate();
	}
	
	private void parseSlideData() {
		try {
			// Clear existing elements
			elements.clear();
			
			// Set background color
			String bgColor = slideData.optString("backgroundColor", "#FFFFFF");
			backgroundColor = Color.parseColor(bgColor);
			
			// Use ElementFactory to create elements
			elements = ElementFactory.createElementsFromJSON(slideData, context, imageCache);
		} catch (Exception e) {
			Log.e("SlideRenderer", "Error parsing slide data", e);
		}
	}
	
	public boolean handleTouchEvent(MotionEvent event) {
		// Let scale gesture detector handle pinch events
		boolean scaleHandled = scaleGestureDetector.onTouchEvent(event);
		
		// If we're in a scaling operation, don't process other gestures
		if (scaleGestureDetector.isInProgress()) {
			return true;
		}
		
		// Let gesture detector handle double-tap and scroll events when not in edit mode
		boolean gestureHandled = gestureDetector.onTouchEvent(event);
		
		float rawX = event.getX();
		float rawY = event.getY();
		
		// Convert screen coordinates to slide coordinates
		float x = (rawX - translateX) / scaleFactor;
		float y = (rawY - translateY) / scaleFactor;
		
		switch (event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				// Reset snap state
				isSnapped = false;
				
				// First check if touching resize handle of selected element
				if (selectedElement != null) {
					float[] handles = {
						selectedElement.x, selectedElement.y, // top-left
						selectedElement.x + selectedElement.width, selectedElement.y, // top-right
						selectedElement.x, selectedElement.y + selectedElement.height, // bottom-left
						selectedElement.x + selectedElement.width, selectedElement.y + selectedElement.height // bottom-right
					};
					
					for (int i = 0; i < handles.length; i += 2) {
						if (Math.abs(x - handles[i]) < HANDLE_EDGE_THRESHOLD / scaleFactor &&
							Math.abs(y - handles[i + 1]) < HANDLE_EDGE_THRESHOLD / scaleFactor) {
							isResizing = true;
							resizeHandleIndex = i / 2;
							lastTouchX = x;
							lastTouchY = y;
							return true;
						}
					}
					
					// If not touching a resize handle, check if touching the selected element for moving
					if (selectedElement.containsPoint(x, y)) {
						isMovingElement = true;
						lastTouchX = x;
						lastTouchY = y;
						return true;
					}
				}
				
				// If not touching selected element or its handles, check for element selection
				SlideElement tappedElement = findElementAt(x, y);
				if (tappedElement != null) {
					// Select the element
					setSelectedElement(tappedElement);
					if (elementSelectionListener != null) {
						elementSelectionListener.onElementSelected(tappedElement);
					}
					lastTouchX = x;
					lastTouchY = y;
					return true;
				} else {
					// Tapped on empty space, deselect current element
					setSelectedElement(null);
				}
				break;
				
			case MotionEvent.ACTION_MOVE:
				// Handle element resizing
				if (isResizing && selectedElement != null) {
					float dx = (x - lastTouchX);
					float dy = (y - lastTouchY);
					
					// Store original values for text scaling calculation
					float originalWidth = selectedElement.width;
					float originalHeight = selectedElement.height;
					
					switch (resizeHandleIndex) {
						case 0: // Top-left
							selectedElement.x += dx;
							selectedElement.y += dy;
							selectedElement.width -= dx;
							selectedElement.height -= dy;
							break;
						case 1: // Top-right
							selectedElement.y += dy;
							selectedElement.width += dx;
							selectedElement.height -= dy;
							break;
						case 2: // Bottom-left
							selectedElement.x += dx;
							selectedElement.width -= dx;
							selectedElement.height += dy;
							break;
						case 3: // Bottom-right
							selectedElement.width += dx;
							selectedElement.height += dy;
							break;
					}
					
					// Ensure minimum size
					if (selectedElement.width < 10) selectedElement.width = 10;
					if (selectedElement.height < 10) selectedElement.height = 10;
					
					// Handle text element resizing - scale text size proportionally
					if (selectedElement instanceof TextElement) {
						TextElement textElement = (TextElement) selectedElement;
						float widthRatio = selectedElement.width / originalWidth;
						float heightRatio = selectedElement.height / originalHeight;
						
						// Use the average of width and height ratios for text scaling
						float scaleRatio = (widthRatio + heightRatio) / 2.0f;
						
						// Scale the text size (corrected line)
						textElement.fontSize = textElement.fontSize * scaleRatio;
						
						// Recreate the text layout with new size
						textElement.createTextLayout();
					}
					
					// Check for alignment with other elements and snap if close
					if (showAlignmentGuides) {
						checkAlignmentGuides(selectedElement);
					}
					
					lastTouchX = x;
					lastTouchY = y;
					slideView.invalidate();
					return true;
				}
				
				// Handle element moving
				if (isMovingElement && selectedElement != null) {
					float dx = (x - lastTouchX);
					float dy = (y - lastTouchY);
					
					// Calculate total movement from initial touch
					float totalDx = x - lastTouchX;
					float totalDy = y - lastTouchY;
					float totalDistance = (float) Math.sqrt(totalDx * totalDx + totalDy * totalDy);
					
					// If movement exceeds threshold, break the snap
					if (isSnapped && totalDistance > snapBreakThreshold / scaleFactor) {
						isSnapped = false;
						horizontalGuides.clear();
						verticalGuides.clear();
					}
					
					selectedElement.x += dx;
					selectedElement.y += dy;
					
					// Check for alignment with other elements and show guides
					if (showAlignmentGuides && !isSnapped) {
						checkAlignmentGuides(selectedElement);
					}
					
					lastTouchX = x;
					lastTouchY = y;
					slideView.invalidate();
					return true;
				}
				break;
				
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				// Reset flags
				isResizing = false;
				isMovingElement = false;
				resizeHandleIndex = -1;
				isSnapped = false;
				
				// Clear alignment guides
				horizontalGuides.clear();
				verticalGuides.clear();
				slideView.invalidate();
				break;
		}
		
		// If we're in edit mode and have a selected element, don't let the gesture detector handle panning
		if (isInEditMode && selectedElement != null) {
			return true;
		}
		
		return gestureHandled || scaleHandled;
	}
	
	/**
	 * Check for alignment with other elements and show guides
	 */
	private void checkAlignmentGuides(SlideElement element) {
		// Clear previous guides
		horizontalGuides.clear();
		verticalGuides.clear();
		
		// Calculate element edges
		float left = element.x;
		float right = element.x + element.width;
		float top = element.y;
		float bottom = element.y + element.height;
		float centerX = element.x + element.width / 2;
		float centerY = element.y + element.height / 2;
		
		// Check alignment with other elements
		for (SlideElement other : elements) {
			if (other == element) continue;
			
			float otherLeft = other.x;
			float otherRight = other.x + other.width;
			float otherTop = other.y;
			float otherBottom = other.y + other.height;
			float otherCenterX = other.x + other.width / 2;
			float otherCenterY = other.y + other.height / 2;
			
			// Check horizontal alignment (left, center, right)
			checkAndShowHorizontal(left, otherLeft, element, -1, 0);
			checkAndShowHorizontal(centerX, otherCenterX, element, 0, 0);
			checkAndShowHorizontal(right, otherRight, element, 1, 0);
			
			// Also check left to right and right to left
			checkAndShowHorizontal(left, otherRight, element, -1, 0);
			checkAndShowHorizontal(right, otherLeft, element, 1, 0);
			
			// Check vertical alignment (top, center, bottom)
			checkAndShowVertical(top, otherTop, element, -1, 0);
			checkAndShowVertical(centerY, otherCenterY, element, 0, 0);
			checkAndShowVertical(bottom, otherBottom, element, 1, 0);
			
			// Also check top to bottom and bottom to top
			checkAndShowVertical(top, otherBottom, element, -1, 0);
			checkAndShowVertical(bottom, otherTop, element, 1, 0);
		}
	}
	
	private void checkAndShowHorizontal(float value1, float value2, SlideElement element, int edge, float offset) {
		if (Math.abs(value1 - value2) < snapThreshold / scaleFactor) {
			// Add guide for visualization
			verticalGuides.add(value2);
		}
	}
	
	private void checkAndShowVertical(float value1, float value2, SlideElement element, int edge, float offset) {
		if (Math.abs(value1 - value2) < snapThreshold / scaleFactor) {
			// Add guide for visualization
			horizontalGuides.add(value2);
		}
	}
	
	private SlideElement findElementAt(float x, float y) {
		// Check elements in reverse order (top-most first)
		for (int i = elements.size() - 1; i >= 0; i--) {
			SlideElement element = elements.get(i);
			if (element.containsPoint(x, y)) {
				return element;
			}
		}
		return null;
	}
	
	public void draw(Canvas canvas) {
		// Apply transformation matrix for zoom and pan
		canvas.save();
		canvas.translate(translateX, translateY);
		canvas.scale(scaleFactor, scaleFactor);
		
		// Draw background
		canvas.drawColor(backgroundColor);
		
		// Draw all elements
		for (SlideElement element : elements) {
			element.draw(canvas);
		}
		
		// Draw selection border and resize handles for selected element
		if (selectedElement != null) {
			// Create selection rectangle
			RectF selectionRect = new RectF(
				selectedElement.x - dpToPx(2) / scaleFactor, 
				selectedElement.y - dpToPx(2) / scaleFactor, 
				selectedElement.x + selectedElement.width + dpToPx(2) / scaleFactor, 
				selectedElement.y + selectedElement.height + dpToPx(2) / scaleFactor
			);
			
			// Draw selection border - improved style with dashed effect
			Paint selectionPaint = new Paint();
			selectionPaint.setStyle(Paint.Style.STROKE);
			selectionPaint.setColor(Color.BLUE); // Changed to solid blue for better visibility
			selectionPaint.setStrokeWidth(dpToPx(1.5f) / scaleFactor);
			selectionPaint.setPathEffect(new DashPathEffect(new float[] {dpToPx(4) / scaleFactor, dpToPx(2) / scaleFactor}, 0));
			
			// Draw selection rectangle
			canvas.drawRect(selectionRect, selectionPaint);
			
			// Draw solid border underneath
			Paint solidBorderPaint = new Paint();
			solidBorderPaint.setStyle(Paint.Style.STROKE);
			solidBorderPaint.setColor(Color.WHITE);
			solidBorderPaint.setStrokeWidth(dpToPx(1) / scaleFactor);
			canvas.drawRect(selectionRect, solidBorderPaint);
			
			// Draw resize handles - smaller and more elegant
			float handleRadius = dpToPx(HANDLE_SIZE) / (2 * scaleFactor);
			float[] handles = {
				selectedElement.x, selectedElement.y, // top-left
				selectedElement.x + selectedElement.width, selectedElement.y, // top-right
				selectedElement.x, selectedElement.y + selectedElement.height, // bottom-left
				selectedElement.x + selectedElement.width, selectedElement.y + selectedElement.height // bottom-right
			};
			
			// Handle fill paint
			Paint handleFillPaint = new Paint();
			handleFillPaint.setColor(Color.BLUE); // Changed to blue for better visibility
			handleFillPaint.setStyle(Paint.Style.FILL);
			
			// Handle stroke paint
			Paint handleStrokePaint = new Paint();
			handleStrokePaint.setColor(Color.WHITE);
			handleStrokePaint.setStyle(Paint.Style.STROKE);
			handleStrokePaint.setStrokeWidth(dpToPx(1) / scaleFactor);
			
			for (int i = 0; i < handles.length; i += 2) {
				// Draw white border around handle
				canvas.drawCircle(handles[i], handles[i + 1], handleRadius + dpToPx(1) / scaleFactor, handleStrokePaint);
				// Draw handle fill
				canvas.drawCircle(handles[i], handles[i + 1], handleRadius, handleFillPaint);
			}
		}
		
		// Draw alignment guides if any
		if (showAlignmentGuides && (isMovingElement || isResizing) && (!horizontalGuides.isEmpty() || !verticalGuides.isEmpty())) {
			Paint guidePaint = new Paint();
			guidePaint.setColor(Color.BLUE); // Changed to blue for better visibility
			guidePaint.setStrokeWidth(dpToPx(1) / scaleFactor);
			guidePaint.setStyle(Paint.Style.STROKE);
			guidePaint.setPathEffect(new DashPathEffect(new float[] {dpToPx(4) / scaleFactor, dpToPx(2) / scaleFactor}, 0));
			
			// Draw horizontal guides
			for (Float y : horizontalGuides) {
				canvas.drawLine(0, y, 10000, y, guidePaint);
			}
			
			// Draw vertical guides
			for (Float x : verticalGuides) {
				canvas.drawLine(x, 0, x, 10000, guidePaint);
			}
		}
		
		canvas.restore();
	}
	
	public SlideElement getSelectedElement() {
		return selectedElement;
	}
	
	public void setSelectedElement(SlideElement element) {
		selectedElement = element;
		isInEditMode = (element != null);
		slideView.invalidate();
	}
	
	private int dpToPx(float dp) {
		return (int) (dp * context.getResources().getDisplayMetrics().density);
	}
	
	/**
	* Scale gesture listener for zoom functionality
	*/
	private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			float previousScale = scaleFactor;
			scaleFactor *= detector.getScaleFactor();
			
			// Limit scale range
			scaleFactor = Math.max(minScale, Math.min(scaleFactor, maxScale));
			
			// Adjust translation to keep focus point stationary
			if (previousScale != scaleFactor) {
				float focusX = detector.getFocusX();
				float focusY = detector.getFocusY();
				
				// Calculate the focus point in the slide's coordinate system
				float focusXInSlide = (focusX - translateX) / previousScale;
				float focusYInSlide = (focusY - translateY) / previousScale;
				
				// Adjust translation to keep the focus point stationary
				translateX = focusX - focusXInSlide * scaleFactor;
				translateY = focusY - focusYInSlide * scaleFactor;
				
				slideView.invalidate();
			}
			
			return true;
		}
	}
	
	/**
	* Gesture listener for pan functionality
	*/
	private class GestureListener extends GestureDetector.SimpleOnGestureListener {
		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			// Only pan if not editing an element
			if (!isInEditMode) {
				translateX -= distanceX;
				translateY -= distanceY;
				slideView.invalidate();
				return true;
			}
			return false;
		}
		
		@Override
		public boolean onDoubleTap(MotionEvent e) {
			// Reset zoom and pan on double tap
			resetTransformation();
			return true;
		}
		
		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			// Convert screen coordinates to slide coordinates
			float x = (e.getX() - translateX) / scaleFactor;
			float y = (e.getY() - translateY) / scaleFactor;
			
			// Check if tapped on an element
			SlideElement tappedElement = findElementAt(x, y);
			
			// Update selection
			if (tappedElement != null) {
				setSelectedElement(tappedElement);
				if (elementSelectionListener != null) {
					elementSelectionListener.onElementSelected(tappedElement);
				}
				return true;
			} else {
				// Tapped on empty space, deselect current element
				setSelectedElement(null);
				return true;
			}
		}
	}
}
