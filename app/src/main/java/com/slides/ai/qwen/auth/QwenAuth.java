package com.slides.ai.qwen.auth;

import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class QwenAuth {

    private static final String QWEN_OAUTH_BASE_URL = "https://chat.qwen.ai";
    private static final String QWEN_OAUTH_DEVICE_CODE_ENDPOINT = QWEN_OAUTH_BASE_URL + "/api/v1/oauth2/device/code";
    private static final String QWEN_OAUTH_TOKEN_ENDPOINT = QWEN_OAUTH_BASE_URL + "/api/v1/oauth2/token";

    private static final String QWEN_OAUTH_CLIENT_ID = "f0304373b74a44d2b584a3fb70ca9e56";
    private static final String QWEN_OAUTH_SCOPE = "openid profile email model.completion";
    private static final String QWEN_OAUTH_GRANT_TYPE = "urn:ietf:params:oauth:grant-type:device_code";

    private final OkHttpClient httpClient;
    private final Gson gson = new Gson();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();


    public QwenAuth() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public interface AuthCallback<T> {
        void onSuccess(T response);
        void onError(String message);
    }

    // --- PKCE Generation ---
    public static class PKCE {
        public final String codeVerifier;
        public final String codeChallenge;

        public PKCE(String codeVerifier, String codeChallenge) {
            this.codeVerifier = codeVerifier;
            this.codeChallenge = codeChallenge;
        }
    }

    public PKCE generatePKCEPair() {
        SecureRandom random = new SecureRandom();
        byte[] verifierBytes = new byte[64];
        random.nextBytes(verifierBytes);
        String codeVerifier = Base64.encodeToString(verifierBytes, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);

        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] challengeBytes = sha256.digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
            String codeChallenge = Base64.encodeToString(challengeBytes, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
            return new PKCE(codeVerifier, codeChallenge);
        } catch (NoSuchAlgorithmException e) {
            // Should not happen as SHA-256 is standard
            throw new RuntimeException("SHA-256 not supported", e);
        }
    }

    // --- Device Authorization ---
    public static class DeviceAuthResponse {
        @SerializedName("device_code")
        public String deviceCode;
        @SerializedName("user_code")
        public String userCode;
        @SerializedName("verification_uri")
        public String verificationUri;
        @SerializedName("expires_in")
        public int expiresIn;
        @SerializedName("interval")
        public int interval;
    }

    public void requestDeviceAuthorization(PKCE pkce, AuthCallback<DeviceAuthResponse> callback) {
        executor.execute(() -> {
            FormBody.Builder formBuilder = new FormBody.Builder()
                    .add("client_id", QWEN_OAUTH_CLIENT_ID)
                    .add("scope", QWEN_OAUTH_SCOPE)
                    .add("code_challenge", pkce.codeChallenge)
                    .add("code_challenge_method", "S256");

            Request request = new Request.Builder()
                    .url(QWEN_OAUTH_DEVICE_CODE_ENDPOINT)
                    .post(formBuilder.build())
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .addHeader("Accept", "application/json")
                    .addHeader("x-request-id", UUID.randomUUID().toString())
                    .build();

            try {
                Response response = httpClient.newCall(request).execute();
                ResponseBody body = response.body();
                if (response.isSuccessful() && body != null) {
                    DeviceAuthResponse authResponse = gson.fromJson(body.string(), DeviceAuthResponse.class);
                    mainHandler.post(() -> callback.onSuccess(authResponse));
                } else {
                    String errorBody = body != null ? body.string() : "Unknown error";
                    mainHandler.post(() -> callback.onError("Device authorization failed " + response.code() + ": " + errorBody));
                }
            } catch (IOException e) {
                mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
            }
        });
    }

    // --- Token Polling ---
    public static class TokenResponse {
        @SerializedName("access_token")
        public String accessToken;
        @SerializedName("refresh_token")
        public String refreshToken;
        @SerializedName("token_type")
        public String tokenType;
        @SerializedName("expires_in")
        public int expiresIn;
        @SerializedName("resource")
        public String resource; // Corresponds to resource_url in g4f
        public long expiryDate; // Calculated field
    }
     public static class TokenErrorResponse {
        @SerializedName("error")
        public String error;
        @SerializedName("error_description")
        public String errorDescription;
    }


    public void pollDeviceToken(String deviceCode, String codeVerifier, AuthCallback<TokenResponse> callback) {
         executor.execute(() -> {
            RequestBody requestBody = new FormBody.Builder()
                .add("grant_type", QWEN_OAUTH_GRANT_TYPE)
                .add("client_id", QWEN_OAUTH_CLIENT_ID)
                .add("device_code", deviceCode)
                .add("code_verifier", codeVerifier)
                .build();

            Request request = new Request.Builder()
                .url(QWEN_OAUTH_TOKEN_ENDPOINT)
                .post(requestBody)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Accept", "application/json")
                .build();

            try {
                Response response = httpClient.newCall(request).execute();
                String responseBody = response.body().string();

                if (response.isSuccessful()) {
                    TokenResponse tokenResponse = gson.fromJson(responseBody, TokenResponse.class);
                    tokenResponse.expiryDate = System.currentTimeMillis() + (tokenResponse.expiresIn * 1000);
                    mainHandler.post(() -> callback.onSuccess(tokenResponse));
                } else {
                     TokenErrorResponse errorResponse = gson.fromJson(responseBody, TokenErrorResponse.class);
                     if ("authorization_pending".equals(errorResponse.error)) {
                         // Not an error, just need to keep polling. We can handle this logic in the calling class.
                         mainHandler.post(() -> callback.onError("pending"));
                     } else {
                         mainHandler.post(() -> callback.onError("Token poll failed: " + errorResponse.errorDescription));
                     }
                }
            } catch (IOException e) {
                 mainHandler.post(() -> callback.onError("Network error during token poll: " + e.getMessage()));
            }
        });
    }

    // --- Refresh Token ---
    public void refreshAccessToken(String refreshToken, AuthCallback<TokenResponse> callback) {
         executor.execute(() -> {
            RequestBody requestBody = new FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)
                .add("client_id", QWEN_OAUTH_CLIENT_ID)
                .build();

            Request request = new Request.Builder()
                .url(QWEN_OAUTH_TOKEN_ENDPOINT)
                .post(requestBody)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Accept", "application/json")
                .build();

            try {
                Response response = httpClient.newCall(request).execute();
                 String responseBody = response.body().string();
                if (response.isSuccessful()) {
                    TokenResponse tokenResponse = gson.fromJson(responseBody, TokenResponse.class);
                    tokenResponse.expiryDate = System.currentTimeMillis() + (tokenResponse.expiresIn * 1000);
                    mainHandler.post(() -> callback.onSuccess(tokenResponse));
                } else {
                    mainHandler.post(() -> callback.onError("Token refresh failed " + response.code() + ": " + responseBody));
                }
            } catch (IOException e) {
                 mainHandler.post(() -> callback.onError("Network error during token refresh: " + e.getMessage()));
            }
        });
    }
}
