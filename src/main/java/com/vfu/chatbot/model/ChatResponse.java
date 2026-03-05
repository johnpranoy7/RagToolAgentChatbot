package com.vfu.chatbot.model;


public record ChatResponse(
        String content,
        double confidence,
        String source,
        String sessionId
) {
    public static ChatResponse of(String content, double confidence, String source, String sessionId) {
        return new ChatResponse(content, confidence, source, sessionId);
    }
}
