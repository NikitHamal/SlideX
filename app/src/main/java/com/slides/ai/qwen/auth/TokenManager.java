package com.slides.ai.qwen.auth;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

public class TokenManager {

    private static final String PREF_NAME = "qwen_auth";
    private static final String KEY_TOKEN = "qwen_token";
    private static final long TOKEN_REFRESH_BUFFER_MS = 30 * 1000; // 30 seconds

    private final SharedPreferences sharedPreferences;
    private final Gson gson = new Gson();
    private final QwenAuth qwenAuth;

    public TokenManager(Context context) {
        this.sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.qwenAuth = new QwenAuth();
    }

    public void saveToken(QwenAuth.TokenResponse token) {
        String tokenJson = gson.toJson(token);
        sharedPreferences.edit().putString(KEY_TOKEN, tokenJson).apply();
    }

    public QwenAuth.TokenResponse getToken() {
        String tokenJson = sharedPreferences.getString(KEY_TOKEN, null);
        if (tokenJson == null) {
            return null;
        }
        return gson.fromJson(tokenJson, QwenAuth.TokenResponse.class);
    }

    public void clearToken() {
        sharedPreferences.edit().remove(KEY_TOKEN).apply();
    }

    public boolean isTokenValid() {
        QwenAuth.TokenResponse token = getToken();
        if (token == null) {
            return false;
        }
        return System.currentTimeMillis() < token.expiryDate - TOKEN_REFRESH_BUFFER_MS;
    }

    public interface TokenCallback {
        void onTokenAvailable(String accessToken);
        void onError(String message);
    }

    public void getValidAccessToken(TokenCallback callback) {
        QwenAuth.TokenResponse token = getToken();

        if (token == null) {
            callback.onError("No token available. Please log in.");
            return;
        }

        if (isTokenValid()) {
            callback.onTokenAvailable(token.accessToken);
        } else {
            // Token has expired, try to refresh it
            qwenAuth.refreshAccessToken(token.refreshToken, new QwenAuth.AuthCallback<QwenAuth.TokenResponse>() {
                @Override
                public void onSuccess(QwenAuth.TokenResponse response) {
                    saveToken(response);
                    callback.onTokenAvailable(response.accessToken);
                }

                @Override
                public void onError(String message) {
                    // Refresh failed, clear the invalid token
                    clearToken();
                    callback.onError("Session expired. Please log in again.");
                }
            });
        }
    }
}
