package com.slides.ai.qwen;

public class QwenNewChatResponse {
    public boolean success;
    public String request_id;
    public Data data;

    public static class Data {
        public String id;
    }
}
