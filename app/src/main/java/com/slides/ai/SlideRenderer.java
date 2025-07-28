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
import android.graphics.BlurMaskFilter;
import android.graphics.Rect;

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

	// Callback interface for element updates
	public interface ElementUpdateListener {
		void onElementUpdated();
	}

	private ElementUpdateListener elementUpdateListener;
	
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

	public void setElementUpdateListener(ElementUpdateListener listener) {
		this.elementUpdateListener = listener;
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
                    if (elementUpdateListener != null) {
                        elementUpdateListener.onElementUpdated();
                    }
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
                    if (elementUpdateListener != null) {
                        elementUpdateListener.onElementUpdated();
                    }
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
		
		// Draw selection overlay for selected element
		if (selectedElement != null) {
			// Modern selection with shadow and glow effect
			RectF elementRect = new RectF(
				selectedElement.x,
				selectedElement.y,
				selectedElement.x + selectedElement.width,
				selectedElement.y + selectedElement.height
			);
			
			// Draw shadow/glow effect around selected element
			Paint shadowPaint = new Paint();
			shadowPaint.setColor(Color.parseColor("#4D2196F3")); // Semi-transparent blue
			shadowPaint.setStyle(Paint.Style.FILL);
			shadowPaint.setMaskFilter(new BlurMaskFilter(dpToPx(8) / scaleFactor, BlurMaskFilter.Blur.OUTER));
			
			RectF shadowRect = new RectF(
				elementRect.left - dpToPx(4) / scaleFactor,
				elementRect.top - dpToPx(4) / scaleFactor,
				elementRect.right + dpToPx(4) / scaleFactor,
				elementRect.bottom + dpToPx(4) / scaleFactor
			);
			canvas.drawRect(shadowRect, shadowPaint);
			
			// Draw modern selection border with rounded corners
			Paint selectionPaint = new Paint();
			selectionPaint.setStyle(Paint.Style.STROKE);
			selectionPaint.setColor(Color.parseColor("#2196F3")); // Material Blue
			selectionPaint.setStrokeWidth(dpToPx(2) / scaleFactor);
			selectionPaint.setAntiAlias(true);
			
			RectF selectionRect = new RectF(
				elementRect.left - dpToPx(1) / scaleFactor,
				elementRect.top - dpToPx(1) / scaleFactor,
				elementRect.right + dpToPx(1) / scaleFactor,
				elementRect.bottom + dpToPx(1) / scaleFactor
			);
			
			float cornerRadius = dpToPx(4) / scaleFactor;
			canvas.drawRoundRect(selectionRect, cornerRadius, cornerRadius, selectionPaint);
			
			// Draw modern resize handles with better design
			drawModernResizeHandles(canvas, elementRect);
			
			// Draw rotation handle for improved UX
			drawRotationHandle(canvas, elementRect);
		}
		
		// Draw modern alignment guides
		if (showAlignmentGuides && (isMovingElement || isResizing) && (!horizontalGuides.isEmpty() || !verticalGuides.isEmpty())) {
			drawModernAlignmentGuides(canvas);
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

	/**
	 * Draw modern, Material Design-inspired resize handles
	 */
	private void drawModernResizeHandles(Canvas canvas, RectF elementRect) {
		float handleSize = dpToPx(10) / scaleFactor;
		float handleStroke = dpToPx(2) / scaleFactor;
		
		// Handle positions: corners and mid-points for better control
		float[][] handlePositions = {
			{elementRect.left, elementRect.top}, // Top-left
			{elementRect.centerX(), elementRect.top}, // Top-center
			{elementRect.right, elementRect.top}, // Top-right
			{elementRect.right, elementRect.centerY()}, // Right-center
			{elementRect.right, elementRect.bottom}, // Bottom-right
			{elementRect.centerX(), elementRect.bottom}, // Bottom-center
			{elementRect.left, elementRect.bottom}, // Bottom-left
			{elementRect.left, elementRect.centerY()} // Left-center
		};
		
		for (int i = 0; i < handlePositions.length; i++) {
			float x = handlePositions[i][0];
			float y = handlePositions[i][1];
			
			// Draw handle shadow
			Paint shadowPaint = new Paint();
			shadowPaint.setColor(Color.parseColor("#40000000"));
			shadowPaint.setStyle(Paint.Style.FILL);
			shadowPaint.setAntiAlias(true);
			canvas.drawCircle(x + dpToPx(1) / scaleFactor, y + dpToPx(1) / scaleFactor, 
							  handleSize / 2 + handleStroke, shadowPaint);
			
			// Draw handle background
			Paint handleBgPaint = new Paint();
			handleBgPaint.setColor(Color.WHITE);
			handleBgPaint.setStyle(Paint.Style.FILL);
			handleBgPaint.setAntiAlias(true);
			canvas.drawCircle(x, y, handleSize / 2 + handleStroke, handleBgPaint);
			
			// Draw handle border  
			Paint handleBorderPaint = new Paint();
			handleBorderPaint.setColor(Color.parseColor("#2196F3"));
			handleBorderPaint.setStyle(Paint.Style.STROKE);
			handleBorderPaint.setStrokeWidth(handleStroke);
			handleBorderPaint.setAntiAlias(true);
			canvas.drawCircle(x, y, handleSize / 2, handleBorderPaint);
			
			// Draw handle icon based on position
			drawHandleIcon(canvas, x, y, i, handleSize);
		}
	}
	
	/**
	 * Draw icons inside resize handles for better UX
	 */
	private void drawHandleIcon(Canvas canvas, float x, float y, int handleIndex, float handleSize) {
		Paint iconPaint = new Paint();
		iconPaint.setColor(Color.parseColor("#2196F3"));
		iconPaint.setStyle(Paint.Style.STROKE);
		iconPaint.setStrokeWidth(dpToPx(1.5f) / scaleFactor);
		iconPaint.setAntiAlias(true);
		iconPaint.setStrokeCap(Paint.Cap.ROUND);
		
		float iconRadius = handleSize / 4;
		
		switch (handleIndex) {
			case 0: case 4: // Corner handles - diagonal resize icon
				canvas.drawLine(x - iconRadius, y - iconRadius, x + iconRadius, y + iconRadius, iconPaint);
				canvas.drawLine(x - iconRadius, y + iconRadius, x + iconRadius, y - iconRadius, iconPaint);
				break;
			case 1: case 5: // Top/bottom handles - vertical resize icon
				canvas.drawLine(x, y - iconRadius, x, y + iconRadius, iconPaint);
				canvas.drawLine(x - iconRadius/2, y - iconRadius/2, x + iconRadius/2, y - iconRadius/2, iconPaint);
				canvas.drawLine(x - iconRadius/2, y + iconRadius/2, x + iconRadius/2, y + iconRadius/2, iconPaint);
				break;
			case 2: case 6: // Corner handles - diagonal resize icon  
				canvas.drawLine(x - iconRadius, y - iconRadius, x + iconRadius, y + iconRadius, iconPaint);
				canvas.drawLine(x - iconRadius, y + iconRadius, x + iconRadius, y - iconRadius, iconPaint);
				break;
			case 3: case 7: // Left/right handles - horizontal resize icon
				canvas.drawLine(x - iconRadius, y, x + iconRadius, y, iconPaint);
				canvas.drawLine(x - iconRadius/2, y - iconRadius/2, x - iconRadius/2, y + iconRadius/2, iconPaint);
				canvas.drawLine(x + iconRadius/2, y - iconRadius/2, x + iconRadius/2, y + iconRadius/2, iconPaint);
				break;
		}
	}
	
	/**
	 * Draw rotation handle for advanced element manipulation
	 */
	private void drawRotationHandle(Canvas canvas, RectF elementRect) {
		float handleDistance = dpToPx(30) / scaleFactor;
		float rotationX = elementRect.centerX();
		float rotationY = elementRect.top - handleDistance;
		float handleSize = dpToPx(12) / scaleFactor;
		
		// Draw connection line
		Paint linePaint = new Paint();
		linePaint.setColor(Color.parseColor("#2196F3"));
		linePaint.setStrokeWidth(dpToPx(1.5f) / scaleFactor);
		linePaint.setAntiAlias(true);
		canvas.drawLine(elementRect.centerX(), elementRect.top, rotationX, rotationY, linePaint);
		
		// Draw rotation handle
		Paint handlePaint = new Paint();
		handlePaint.setColor(Color.WHITE);
		handlePaint.setStyle(Paint.Style.FILL);
		handlePaint.setAntiAlias(true);
		canvas.drawCircle(rotationX, rotationY, handleSize / 2, handlePaint);
		
		Paint borderPaint = new Paint();
		borderPaint.setColor(Color.parseColor("#2196F3"));
		borderPaint.setStyle(Paint.Style.STROKE);
		borderPaint.setStrokeWidth(dpToPx(2) / scaleFactor);
		borderPaint.setAntiAlias(true);
		canvas.drawCircle(rotationX, rotationY, handleSize / 2, borderPaint);
		
		// Draw rotation icon
		drawRotationIcon(canvas, rotationX, rotationY, handleSize);
	}
	
	/**
	 * Draw rotation icon inside the rotation handle
	 */
	private void drawRotationIcon(Canvas canvas, float x, float y, float handleSize) {
		Paint iconPaint = new Paint();
		iconPaint.setColor(Color.parseColor("#2196F3"));
		iconPaint.setStyle(Paint.Style.STROKE);
		iconPaint.setStrokeWidth(dpToPx(1.5f) / scaleFactor);
		iconPaint.setAntiAlias(true);
		iconPaint.setStrokeCap(Paint.Cap.ROUND);
		
		float radius = handleSize / 3;
		
		// Draw circular arrow
		RectF arcRect = new RectF(x - radius, y - radius, x + radius, y + radius);
		canvas.drawArc(arcRect, -90, 270, false, iconPaint);
		
		// Draw arrow head
		float arrowX = x + radius;
		float arrowY = y;
		float arrowSize = radius / 3;
		canvas.drawLine(arrowX, arrowY, arrowX - arrowSize, arrowY - arrowSize, iconPaint);
		canvas.drawLine(arrowX, arrowY, arrowX - arrowSize, arrowY + arrowSize, iconPaint);
	}
	
	/**
	 * Draw modern alignment guides with better visual design
	 */
	private void drawModernAlignmentGuides(Canvas canvas) {
		// Create guide paint with modern styling
		Paint guidePaint = new Paint();
		guidePaint.setColor(Color.parseColor("#FF4081")); // Material Pink for contrast
		guidePaint.setStrokeWidth(dpToPx(1.5f) / scaleFactor);
		guidePaint.setStyle(Paint.Style.STROKE);
		guidePaint.setAntiAlias(true);
		
		// Create animation effect for guides
		long currentTime = System.currentTimeMillis();
		float phase = (currentTime % 1000) / 1000f * dpToPx(8) / scaleFactor;
		guidePaint.setPathEffect(new DashPathEffect(new float[] {
			dpToPx(6) / scaleFactor, dpToPx(4) / scaleFactor
		}, phase));
		
		// Draw horizontal guides with fade effect at edges
		for (Float y : horizontalGuides) {
			// Draw full guide line
			canvas.drawLine(0, y, canvas.getWidth(), y, guidePaint);
			
			// Draw emphasis dots at intersections
			for (Float verticalX : verticalGuides) {
				drawGuideIntersection(canvas, verticalX, y);
			}
		}
		
		// Draw vertical guides
		for (Float x : verticalGuides) {
			canvas.drawLine(x, 0, x, canvas.getHeight(), guidePaint);
		}
		
		// Draw guide labels for better understanding
		drawGuideLabels(canvas);
	}
	
	/**
	 * Draw intersection points where guides meet
	 */
	private void drawGuideIntersection(Canvas canvas, float x, float y) {
		Paint intersectionPaint = new Paint();
		intersectionPaint.setColor(Color.parseColor("#FF4081"));
		intersectionPaint.setStyle(Paint.Style.FILL);
		intersectionPaint.setAntiAlias(true);
		
		float dotRadius = dpToPx(3) / scaleFactor;
		canvas.drawCircle(x, y, dotRadius, intersectionPaint);
		
		// Add white border for visibility
		Paint borderPaint = new Paint();
		borderPaint.setColor(Color.WHITE);
		borderPaint.setStyle(Paint.Style.STROKE);
		borderPaint.setStrokeWidth(dpToPx(1) / scaleFactor);
		borderPaint.setAntiAlias(true);
		canvas.drawCircle(x, y, dotRadius, borderPaint);
	}
	
	/**
	 * Draw helpful labels for alignment guides
	 */
	private void drawGuideLabels(Canvas canvas) {
		if (selectedElement == null) return;
		
		Paint labelPaint = new Paint();
		labelPaint.setColor(Color.parseColor("#FF4081"));
		labelPaint.setTextSize(dpToPx(10) / scaleFactor);
		labelPaint.setAntiAlias(true);
		labelPaint.setTextAlign(Paint.Align.CENTER);
		
		Paint labelBgPaint = new Paint();
		labelBgPaint.setColor(Color.parseColor("#E0FFFFFF"));
		labelBgPaint.setStyle(Paint.Style.FILL);
		labelBgPaint.setAntiAlias(true);
		
		// Show alignment type at element position
		String alignmentText = getAlignmentDescription();
		if (!alignmentText.isEmpty()) {
			float textX = selectedElement.x + selectedElement.width / 2;
			float textY = selectedElement.y - dpToPx(20) / scaleFactor;
			
			// Draw background
			Rect textBounds = new Rect();
			labelPaint.getTextBounds(alignmentText, 0, alignmentText.length(), textBounds);
			RectF bgRect = new RectF(
				textX - textBounds.width() / 2 - dpToPx(4) / scaleFactor,
				textY - textBounds.height() - dpToPx(2) / scaleFactor,
				textX + textBounds.width() / 2 + dpToPx(4) / scaleFactor,
				textY + dpToPx(2) / scaleFactor
			);
			canvas.drawRoundRect(bgRect, dpToPx(2) / scaleFactor, dpToPx(2) / scaleFactor, labelBgPaint);
			
			// Draw text
			canvas.drawText(alignmentText, textX, textY, labelPaint);
		}
	}
	
	/**
	 * Get description of current alignment for user feedback
	 */
	private String getAlignmentDescription() {
		if (horizontalGuides.isEmpty() && verticalGuides.isEmpty()) {
			return "";
		}
		
		List<String> alignments = new ArrayList<>();
		if (!horizontalGuides.isEmpty()) {
			alignments.add("H-Aligned");
		}
		if (!verticalGuides.isEmpty()) {
			alignments.add("V-Aligned");
		}
		
		return String.join(", ", alignments);
	}
}
