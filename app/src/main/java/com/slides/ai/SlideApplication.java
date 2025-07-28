package com.slides.ai;

import android.app.Application;
import android.content.Intent;

public class SlideApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            Intent intent = new Intent(this, DebugActivity.class);
            intent.putExtra(DebugActivity.EXTRA_LOGS, getStackTrace(ex));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            System.exit(1);
        });

        // Initialize theme manager
        ThemeManager.applyTheme(this);
    }

    private String getStackTrace(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.toString()).append("\n");
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString();
    }
}