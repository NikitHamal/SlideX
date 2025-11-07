package com.slides.ai.qwen.models;

import java.util.List;

public class ChatCompletionResponse {
    public String id;
    public String object;
    public long created;
    public String model;
    public List<Choice> choices;
    public Usage usage;

    public static class Choice {
        public int index;
        public Delta delta;
        public String finish_reason;
    }

    public static class Delta {
        public String role;
        public String content;
    }

    public static class Usage {
        public int prompt_tokens;
        public int completion_tokens;
        public int total_tokens;
    }
}
