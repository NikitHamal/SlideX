package com.slides.ai.qwen;

import java.util.List;

public class QwenCompletionRequest {
    public boolean stream;
    public boolean incremental_output;
    public String chat_id;
    public String chat_mode;
    public String model;
    public String parent_id;
    public List<Message> messages;
    public long timestamp;

    public static class Message {
        public String fid;
        public String parentId;
        public List<String> childrenIds;
        public String role;
        public String content;
        public String user_action;
        public List<File> files;
        public long timestamp;
        public List<String> models;
        public String chat_type;
        public FeatureConfig feature_config;
        public Extra extra;
        public String sub_chat_type;
    }

    public static class File {
        // Define file properties if needed for file uploads
    }

    public static class FeatureConfig {
        public boolean thinking_enabled;
        public String output_schema;
        public int thinking_budget;
    }

    public static class Extra {
        public Meta meta;
    }

    public static class Meta {
        public String subChatType;
    }
}
