package com.slides.ai;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class ApiKeyManager {
    private static final String PREFS_NAME = "api_keys";
    private static final String KEYS_ARRAY = "keys_array";
    private static final String ENCRYPTION_KEY = "encryption_key";
    
    private SharedPreferences sharedPreferences;
    private Context context;
    private SecretKey secretKey;
    
    public ApiKeyManager(Context context) {
        this.context = context;
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        initializeEncryption();
    }
    
    private void initializeEncryption() {
        String keyString = sharedPreferences.getString(ENCRYPTION_KEY, null);
        if (keyString == null) {
            // Generate new encryption key
            try {
                KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
                keyGenerator.init(256);
                secretKey = keyGenerator.generateKey();
                
                String encodedKey = Base64.encodeToString(secretKey.getEncoded(), Base64.DEFAULT);
                sharedPreferences.edit().putString(ENCRYPTION_KEY, encodedKey).apply();
            } catch (Exception e) {
                e.printStackTrace();
                // Fallback to simple obfuscation if encryption fails
                secretKey = null;
            }
        } else {
            try {
                byte[] keyBytes = Base64.decode(keyString, Base64.DEFAULT);
                secretKey = new SecretKeySpec(keyBytes, "AES");
            } catch (Exception e) {
                e.printStackTrace();
                secretKey = null;
            }
        }
    }
    
    private String encrypt(String plainText) {
        if (secretKey == null) {
            // Simple obfuscation fallback
            return Base64.encodeToString(plainText.getBytes(), Base64.DEFAULT);
        }
        
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());
            return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return Base64.encodeToString(plainText.getBytes(), Base64.DEFAULT);
        }
    }
    
    private String decrypt(String encryptedText) {
        if (secretKey == null) {
            // Simple deobfuscation fallback
            try {
                return new String(Base64.decode(encryptedText, Base64.DEFAULT));
            } catch (Exception e) {
                return encryptedText;
            }
        }
        
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.decode(encryptedText, Base64.DEFAULT));
            return new String(decryptedBytes);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                return new String(Base64.decode(encryptedText, Base64.DEFAULT));
            } catch (Exception e2) {
                return encryptedText;
            }
        }
    }
    
    public void addApiKey(String apiKey, String label) {
        List<ApiKey> keys = getApiKeyObjects();
        
        // Check if key already exists
        for (ApiKey key : keys) {
            if (key.getKey().equals(apiKey)) {
                return; // Key already exists
            }
        }
        
        ApiKey newKey = new ApiKey(
            "key_" + System.currentTimeMillis(),
            label.isEmpty() ? "API Key " + (keys.size() + 1) : label,
            apiKey,
            System.currentTimeMillis(),
            true
        );
        
        keys.add(newKey);
        saveApiKeys(keys);
    }
    
    public void removeApiKey(String keyId) {
        List<ApiKey> keys = getApiKeyObjects();
        keys.removeIf(key -> key.getId().equals(keyId));
        saveApiKeys(keys);
    }
    
    public void updateApiKey(String keyId, String newLabel, boolean isVisible) {
        List<ApiKey> keys = getApiKeyObjects();
        for (ApiKey key : keys) {
            if (key.getId().equals(keyId)) {
                key.setLabel(newLabel);
                key.setVisible(isVisible);
                break;
            }
        }
        saveApiKeys(keys);
    }
    
    public List<String> getApiKeys() {
        List<String> keys = new ArrayList<>();
        for (ApiKey apiKey : getApiKeyObjects()) {
            keys.add(apiKey.getKey());
        }
        return keys;
    }
    
    public List<ApiKey> getApiKeyObjects() {
        String keysJson = sharedPreferences.getString(KEYS_ARRAY, "[]");
        List<ApiKey> keys = new ArrayList<>();
        
        try {
            JSONArray keysArray = new JSONArray(keysJson);
            for (int i = 0; i < keysArray.length(); i++) {
                JSONObject keyObj = keysArray.getJSONObject(i);
                ApiKey key = ApiKey.fromJson(keyObj, this);
                if (key != null) {
                    keys.add(key);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
        return keys;
    }
    
    private void saveApiKeys(List<ApiKey> keys) {
        JSONArray keysArray = new JSONArray();
        for (ApiKey key : keys) {
            keysArray.put(key.toJson(this));
        }
        
        sharedPreferences.edit()
                .putString(KEYS_ARRAY, keysArray.toString())
                .apply();
    }
    
    public String getActiveApiKey() {
        List<String> keys = getApiKeys();
        return keys.isEmpty() ? null : keys.get(0);
    }
    
    public static class ApiKey {
        private String id;
        private String label;
        private String key;
        private long createdAt;
        private boolean isVisible;
        
        public ApiKey(String id, String label, String key, long createdAt, boolean isVisible) {
            this.id = id;
            this.label = label;
            this.key = key;
            this.createdAt = createdAt;
            this.isVisible = isVisible;
        }
        
        public JSONObject toJson(ApiKeyManager manager) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("id", id);
                obj.put("label", label);
                obj.put("key", manager.encrypt(key));
                obj.put("createdAt", createdAt);
                obj.put("isVisible", isVisible);
                return obj;
            } catch (JSONException e) {
                e.printStackTrace();
                return new JSONObject();
            }
        }
        
        public static ApiKey fromJson(JSONObject obj, ApiKeyManager manager) {
            try {
                String id = obj.getString("id");
                String label = obj.getString("label");
                String encryptedKey = obj.getString("key");
                long createdAt = obj.getLong("createdAt");
                boolean isVisible = obj.optBoolean("isVisible", true);
                
                String decryptedKey = manager.decrypt(encryptedKey);
                
                return new ApiKey(id, label, decryptedKey, createdAt, isVisible);
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }
        
        public String getDisplayKey() {
            if (!isVisible) {
                return "••••••••••••••••••••••••••••••••••••••••";
            }
            
            if (key.length() <= 8) {
                return key;
            }
            
            return key.substring(0, 4) + "••••••••••••••••••••••••••••••••" + key.substring(key.length() - 4);
        }
        
        // Getters and setters
        public String getId() { return id; }
        public String getLabel() { return label; }
        public String getKey() { return key; }
        public long getCreatedAt() { return createdAt; }
        public boolean isVisible() { return isVisible; }
        
        public void setLabel(String label) { this.label = label; }
        public void setVisible(boolean visible) { isVisible = visible; }
    }
}