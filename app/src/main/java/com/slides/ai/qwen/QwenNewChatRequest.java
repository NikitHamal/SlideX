package com.slides.ai.qwen;

import java.util.List;

public class QwenNewChatRequest {
    public String title;
    public List<String> models;
    public String chat_mode;
    public String chat_type;
    public long timestamp;

    public QwenNewChatRequest(String title, List<String> models, String chat_mode, String chat_type, long timestamp) {
        this.title = title;
        this.models = models;
        this.chat_mode = chat_mode;
        this.chat_type = chat_type;
        this.timestamp = timestamp;
    }
}
