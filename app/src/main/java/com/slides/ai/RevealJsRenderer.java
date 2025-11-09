package com.slides.ai;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * RevealJsRenderer - Modern presentation renderer using Reveal.js HTML framework
 * Replaces the legacy Canvas-based SlideRenderer with a WebView-based approach
 *
 * Features:
 * - Reveal.js integration for professional presentations
 * - HTML/CSS rendering instead of Canvas drawing
 * - Full slide navigation and transition support
 * - Export to HTML format
 * - Element editing and customization
 */
public class RevealJsRenderer {
    private static final String TAG = "RevealJsRenderer";

    private Context context;
    private WebView webView;
    private List<JSONObject> slides;
    private int currentSlideIndex = 0;
    private HashMap<String, Bitmap> imageCache;

    // Callback interfaces
    public interface SlideChangeListener {
        void onSlideChanged(int slideIndex);
    }

    public interface ElementSelectionListener {
        void onElementSelected(SlideElement element, int slideIndex);
    }

    public interface RevealReadyListener {
        void onRevealReady();
    }

    private SlideChangeListener slideChangeListener;
    private ElementSelectionListener elementSelectionListener;
    private RevealReadyListener revealReadyListener;

    public RevealJsRenderer(Context context, WebView webView, HashMap<String, Bitmap> imageCache) {
        this.context = context;
        this.webView = webView;
        this.imageCache = imageCache;
        this.slides = new ArrayList<>();

        initializeWebView();
    }

    private void initializeWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        // Add JavaScript interface for Android-WebView communication
        webView.addJavascriptInterface(new WebAppInterface(), "Android");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(TAG, "WebView page finished loading");
                // Inject slides after page loads
                injectSlides();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }
        });

        // Load the template HTML
        loadTemplate();
    }

    private void loadTemplate() {
        try {
            InputStream is = context.getAssets().open("revealjs/template.html");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String htmlContent = new String(buffer, "UTF-8");

            webView.loadDataWithBaseURL("file:///android_asset/revealjs/", htmlContent, "text/html", "UTF-8", null);
        } catch (Exception e) {
            Log.e(TAG, "Error loading template", e);
            // Fallback: load from CDN
            webView.loadUrl("about:blank");
        }
    }

    public void setSlides(List<JSONObject> slides) {
        this.slides = slides;
        if (slides.size() > 0) {
            currentSlideIndex = Math.min(currentSlideIndex, slides.size() - 1);
        }
        injectSlides();
    }

    public void setSlide(JSONObject slide) {
        if (slide != null) {
            this.slides.clear();
            this.slides.add(slide);
            this.currentSlideIndex = 0;
            injectSlides();
        }
    }

    public void addSlide(JSONObject slide) {
        if (slide != null) {
            this.slides.add(slide);
            injectSlides();
        }
    }

    public JSONObject getCurrentSlide() {
        if (currentSlideIndex >= 0 && currentSlideIndex < slides.size()) {
            return slides.get(currentSlideIndex);
        }
        return null;
    }

    public List<JSONObject> getAllSlides() {
        return new ArrayList<>(slides);
    }

    public int getCurrentSlideIndex() {
        return currentSlideIndex;
    }

    public int getTotalSlides() {
        return slides.size();
    }

    public void navigateToSlide(int index) {
        if (index >= 0 && index < slides.size()) {
            currentSlideIndex = index;
            String js = String.format("window.navigateToSlide(%d);", index);
            webView.evaluateJavascript(js, null);
        }
    }

    public void nextSlide() {
        webView.evaluateJavascript("window.nextSlide();", null);
    }

    public void previousSlide() {
        webView.evaluateJavascript("window.previousSlide();", null);
    }

    private void injectSlides() {
        if (slides.isEmpty()) {
            return;
        }

        StringBuilder slidesHtml = new StringBuilder();

        for (int i = 0; i < slides.size(); i++) {
            JSONObject slide = slides.get(i);
            String slideHtml = generateSlideHtml(slide, i);
            slidesHtml.append(slideHtml);
        }

        // Inject slides into the reveal.js container
        String js = String.format(
            "document.getElementById('slides-container').innerHTML = `%s`; " +
            "if (typeof Reveal !== 'undefined' && Reveal.isReady()) { " +
            "  Reveal.sync(); " +
            "  Reveal.slide(%d, 0); " +
            "}",
            slidesHtml.toString().replace("`", "\\`"),
            currentSlideIndex
        );

        webView.evaluateJavascript(js, null);
    }

    private String generateSlideHtml(JSONObject slideData, int slideIndex) {
        try {
            String backgroundColor = slideData.optString("backgroundColor", "#FFFFFF");
            JSONArray elements = slideData.optJSONArray("elements");

            StringBuilder slideHtml = new StringBuilder();
            slideHtml.append(String.format("<section data-slide-index=\"%d\" style=\"background-color: %s; position: relative;\">",
                slideIndex, backgroundColor));

            if (elements != null) {
                for (int i = 0; i < elements.length(); i++) {
                    JSONObject element = elements.getJSONObject(i);
                    String elementHtml = generateElementHtml(element, slideIndex, i);
                    slideHtml.append(elementHtml);
                }
            }

            slideHtml.append("</section>");
            return slideHtml.toString();

        } catch (Exception e) {
            Log.e(TAG, "Error generating slide HTML", e);
            return "<section><p>Error rendering slide</p></section>";
        }
    }

    private String generateElementHtml(JSONObject element, int slideIndex, int elementIndex) {
        try {
            String type = element.optString("type", "text").toLowerCase();

            switch (type) {
                case "text":
                    return generateTextElement(element, slideIndex, elementIndex);
                case "image":
                    return generateImageElement(element, slideIndex, elementIndex);
                case "shape":
                case "rectangle":
                case "oval":
                case "circle":
                case "line":
                case "triangle":
                    return generateShapeElement(element, slideIndex, elementIndex);
                case "table":
                    return generateTableElement(element, slideIndex, elementIndex);
                case "chart":
                    return generateChartElement(element, slideIndex, elementIndex);
                case "icon":
                    return generateIconElement(element, slideIndex, elementIndex);
                default:
                    return "";
            }
        } catch (Exception e) {
            Log.e(TAG, "Error generating element HTML", e);
            return "";
        }
    }

    private String generateTextElement(JSONObject element, int slideIndex, int elementIndex) {
        try {
            String content = element.optString("content", "");
            int x = element.optInt("x", 0);
            int y = element.optInt("y", 0);
            int width = element.optInt("width", 100);
            int height = element.optInt("height", 50);
            int fontSize = element.optInt("fontSize", 16);
            String color = element.optString("color", "#000000");
            boolean bold = element.optBoolean("bold", false);
            boolean italic = element.optBoolean("italic", false);
            String alignment = element.optString("alignment", "left");

            String fontWeight = bold ? "bold" : "normal";
            String fontStyle = italic ? "italic" : "normal";

            return String.format(
                "<div class=\"slide-text text-%s\" data-slide=\"%d\" data-element=\"%d\" " +
                "style=\"left: %ddp; top: %ddp; width: %ddp; height: %ddp; " +
                "font-size: %dpx; color: %s; font-weight: %s; font-style: %s; " +
                "display: flex; align-items: center;\">%s</div>",
                alignment, slideIndex, elementIndex,
                x, y, width, height,
                fontSize, color, fontWeight, fontStyle,
                escapeHtml(content)
            );
        } catch (Exception e) {
            Log.e(TAG, "Error generating text element", e);
            return "";
        }
    }

    private String generateImageElement(JSONObject element, int slideIndex, int elementIndex) {
        try {
            String url = element.optString("url", "");
            int x = element.optInt("x", 0);
            int y = element.optInt("y", 0);
            int width = element.optInt("width", 100);
            int height = element.optInt("height", 100);
            int cornerRadius = element.optInt("cornerRadius", 0);

            // Check if image is in cache and convert to base64
            String imageSrc = url;
            if (imageCache.containsKey(url)) {
                Bitmap bitmap = imageCache.get(url);
                imageSrc = bitmapToBase64(bitmap);
            }

            return String.format(
                "<img class=\"slide-image\" data-slide=\"%d\" data-element=\"%d\" " +
                "src=\"%s\" style=\"left: %ddp; top: %ddp; width: %ddp; height: %ddp; " +
                "border-radius: %dpx;\" />",
                slideIndex, elementIndex,
                imageSrc, x, y, width, height, cornerRadius
            );
        } catch (Exception e) {
            Log.e(TAG, "Error generating image element", e);
            return "";
        }
    }

    private String generateShapeElement(JSONObject element, int slideIndex, int elementIndex) {
        try {
            String shapeType = element.optString("shapeType", element.optString("type", "rectangle"));
            int x = element.optInt("x", 0);
            int y = element.optInt("y", 0);
            int width = element.optInt("width", 100);
            int height = element.optInt("height", 100);
            String color = element.optString("color", "#2196F3");
            int cornerRadius = element.optInt("cornerRadius", 0);
            double opacity = element.optDouble("opacity", 1.0);
            int strokeWidth = element.optInt("strokeWidth", 0);
            String strokeColor = element.optString("strokeColor", "#000000");

            String shapeClass = "shape-" + shapeType.toLowerCase();
            String border = strokeWidth > 0 ? String.format("%dpx solid %s", strokeWidth, strokeColor) : "none";

            if (shapeType.equalsIgnoreCase("triangle")) {
                // Special handling for triangle using CSS borders
                return String.format(
                    "<div class=\"slide-shape %s\" data-slide=\"%d\" data-element=\"%d\" " +
                    "style=\"left: %ddp; top: %ddp; " +
                    "border-left: %ddp solid transparent; " +
                    "border-right: %ddp solid transparent; " +
                    "border-bottom: %ddp solid %s; opacity: %.2f;\"></div>",
                    shapeClass, slideIndex, elementIndex,
                    x, y,
                    width / 2, width / 2, height, color, opacity
                );
            } else {
                return String.format(
                    "<div class=\"slide-shape %s\" data-slide=\"%d\" data-element=\"%d\" " +
                    "style=\"left: %ddp; top: %ddp; width: %ddp; height: %ddp; " +
                    "background-color: %s; opacity: %.2f; border: %s; " +
                    "--corner-radius: %dpx;\"></div>",
                    shapeClass, slideIndex, elementIndex,
                    x, y, width, height,
                    color, opacity, border, cornerRadius
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "Error generating shape element", e);
            return "";
        }
    }

    private String generateTableElement(JSONObject element, int slideIndex, int elementIndex) {
        try {
            int x = element.optInt("x", 0);
            int y = element.optInt("y", 0);
            int width = element.optInt("width", 200);
            int rows = element.optInt("rows", 3);
            int columns = element.optInt("columns", 3);
            String headerColor = element.optString("headerColor", "#E3F2FD");
            String cellColor = element.optString("cellColor", "#FFFFFF");
            String borderColor = element.optString("borderColor", "#2196F3");
            int borderWidth = element.optInt("borderWidth", 1);
            JSONArray data = element.optJSONArray("data");

            StringBuilder tableHtml = new StringBuilder();
            tableHtml.append(String.format(
                "<table class=\"slide-table\" data-slide=\"%d\" data-element=\"%d\" " +
                "style=\"left: %ddp; top: %ddp; width: %ddp; border-color: %s; border-width: %dpx;\">",
                slideIndex, elementIndex, x, y, width, borderColor, borderWidth
            ));

            // Generate table rows
            for (int i = 0; i < rows; i++) {
                tableHtml.append("<tr>");
                for (int j = 0; j < columns; j++) {
                    String cellBg = i == 0 ? headerColor : cellColor;
                    String cellTag = i == 0 ? "th" : "td";
                    String cellContent = "";

                    if (data != null && i < data.length()) {
                        JSONArray row = data.optJSONArray(i);
                        if (row != null && j < row.length()) {
                            cellContent = row.optString(j, "");
                        }
                    }

                    tableHtml.append(String.format(
                        "<%s style=\"background-color: %s; border-color: %s;\">%s</%s>",
                        cellTag, cellBg, borderColor, escapeHtml(cellContent), cellTag
                    ));
                }
                tableHtml.append("</tr>");
            }

            tableHtml.append("</table>");
            return tableHtml.toString();

        } catch (Exception e) {
            Log.e(TAG, "Error generating table element", e);
            return "";
        }
    }

    private String generateChartElement(JSONObject element, int slideIndex, int elementIndex) {
        try {
            int x = element.optInt("x", 0);
            int y = element.optInt("y", 0);
            int width = element.optInt("width", 300);
            int height = element.optInt("height", 250);
            String chartType = element.optString("chartType", "bar");
            boolean showLegend = element.optBoolean("showLegend", true);
            JSONArray data = element.optJSONArray("data");

            StringBuilder chartHtml = new StringBuilder();
            chartHtml.append(String.format(
                "<div class=\"slide-chart\" data-slide=\"%d\" data-element=\"%d\" " +
                "style=\"left: %ddp; top: %ddp; width: %ddp; height: %ddp;\">",
                slideIndex, elementIndex, x, y, width, height
            ));

            if (chartType.equalsIgnoreCase("bar") && data != null) {
                chartHtml.append("<div class=\"chart-bars\">");

                // Find max value for scaling
                double maxValue = 0;
                for (int i = 0; i < data.length(); i++) {
                    JSONObject item = data.optJSONObject(i);
                    if (item != null) {
                        double value = item.optDouble("value", 0);
                        maxValue = Math.max(maxValue, value);
                    }
                }

                // Generate bars
                for (int i = 0; i < data.length(); i++) {
                    JSONObject item = data.optJSONObject(i);
                    if (item != null) {
                        String label = item.optString("label", "");
                        double value = item.optDouble("value", 0);
                        String color = item.optString("color", "#2196F3");

                        double heightPercent = maxValue > 0 ? (value / maxValue) * 100 : 0;

                        chartHtml.append(String.format(
                            "<div class=\"chart-bar\">" +
                            "<div class=\"chart-bar-fill\" style=\"height: %.1f%%; background-color: %s;\"></div>" +
                            "<div class=\"chart-bar-label\">%s</div>" +
                            "</div>",
                            heightPercent, color, escapeHtml(label)
                        ));
                    }
                }

                chartHtml.append("</div>");

                // Add legend if enabled
                if (showLegend) {
                    chartHtml.append("<div class=\"chart-legend\">");
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject item = data.optJSONObject(i);
                        if (item != null) {
                            String label = item.optString("label", "");
                            String color = item.optString("color", "#2196F3");
                            double value = item.optDouble("value", 0);

                            chartHtml.append(String.format(
                                "<div class=\"chart-legend-item\">" +
                                "<div class=\"chart-legend-color\" style=\"background-color: %s;\"></div>" +
                                "<span>%s: %.1f</span>" +
                                "</div>",
                                color, escapeHtml(label), value
                            ));
                        }
                    }
                    chartHtml.append("</div>");
                }
            }

            chartHtml.append("</div>");
            return chartHtml.toString();

        } catch (Exception e) {
            Log.e(TAG, "Error generating chart element", e);
            return "";
        }
    }

    private String generateIconElement(JSONObject element, int slideIndex, int elementIndex) {
        try {
            String iconName = element.optString("iconName", "settings");
            int x = element.optInt("x", 0);
            int y = element.optInt("y", 0);
            int width = element.optInt("width", 40);
            int height = element.optInt("height", 40);
            String color = element.optString("color", "#2196F3");

            // Map icon names to Unicode symbols or emoji
            String iconContent = getIconUnicode(iconName);

            return String.format(
                "<div class=\"slide-icon\" data-slide=\"%d\" data-element=\"%d\" " +
                "style=\"left: %ddp; top: %ddp; width: %ddp; height: %ddp; color: %s; font-size: %dpx;\">%s</div>",
                slideIndex, elementIndex,
                x, y, width, height, color, Math.min(width, height), iconContent
            );
        } catch (Exception e) {
            Log.e(TAG, "Error generating icon element", e);
            return "";
        }
    }

    private String getIconUnicode(String iconName) {
        // Map common icon names to Unicode characters
        switch (iconName.toLowerCase()) {
            case "settings": return "⚙";
            case "check": return "✓";
            case "star": return "★";
            case "heart": return "♥";
            case "arrow": return "→";
            case "info": return "ℹ";
            case "warning": return "⚠";
            case "error": return "✖";
            case "user": return "👤";
            case "home": return "🏠";
            case "email": return "✉";
            case "phone": return "☎";
            case "calendar": return "📅";
            case "clock": return "🕐";
            case "location": return "📍";
            case "search": return "🔍";
            default: return "●";
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;")
                   .replace("\n", "<br>");
    }

    private String bitmapToBase64(Bitmap bitmap) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            String encoded = Base64.encodeToString(byteArray, Base64.NO_WRAP);
            return "data:image/png;base64," + encoded;
        } catch (Exception e) {
            Log.e(TAG, "Error converting bitmap to base64", e);
            return "";
        }
    }

    public String exportToHtml() {
        try {
            StringBuilder html = new StringBuilder();

            // Load template
            InputStream is = context.getAssets().open("revealjs/template.html");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String template = new String(buffer, "UTF-8");

            // Generate all slides HTML
            StringBuilder slidesHtml = new StringBuilder();
            for (int i = 0; i < slides.size(); i++) {
                slidesHtml.append(generateSlideHtml(slides.get(i), i));
            }

            // Replace placeholder in template
            html.append(template.replace("<!-- Slides will be injected here -->", slidesHtml.toString()));

            return html.toString();

        } catch (Exception e) {
            Log.e(TAG, "Error exporting to HTML", e);
            return null;
        }
    }

    public void setSlideChangeListener(SlideChangeListener listener) {
        this.slideChangeListener = listener;
    }

    public void setElementSelectionListener(ElementSelectionListener listener) {
        this.elementSelectionListener = listener;
    }

    public void setRevealReadyListener(RevealReadyListener listener) {
        this.revealReadyListener = listener;
    }

    /**
     * JavaScript interface for Android-WebView communication
     */
    private class WebAppInterface {
        @JavascriptInterface
        public void onSlideChanged(int slideIndex) {
            currentSlideIndex = slideIndex;
            if (slideChangeListener != null) {
                slideChangeListener.onSlideChanged(slideIndex);
            }
        }

        @JavascriptInterface
        public void onRevealReady() {
            Log.d(TAG, "Reveal.js is ready");
            if (revealReadyListener != null) {
                revealReadyListener.onRevealReady();
            }
        }
    }
}
