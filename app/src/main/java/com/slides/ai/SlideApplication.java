package com.slides.ai;

import android.app.Application;

public class SlideApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize theme manager
        ThemeManager.applyTheme(this);
    }
}