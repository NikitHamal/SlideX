package com.slides.ai;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.webkit.WebView;

public class DocumentationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_documentation);

        WebView webView = findViewById(R.id.documentation_webview);
        webView.loadUrl("file:///android_asset/documentation.html");
    }
}
