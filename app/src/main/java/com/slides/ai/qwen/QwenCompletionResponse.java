package com.slides.ai.qwen;

import java.util.List;

public class QwenCompletionResponse {
    public ResponseCreated response_created;
    public List<Choice> choices;

    public static class ResponseCreated {
        public String chat_id;
        public String parent_id;
        public String response_id;
    }

    public static class Choice {
        public Delta delta;
    }

    public static class Delta {
        public String role;
        public String content;
        public String phase;
        public String status;
    }
}
