package com.vfu.chatbot.model;


public record ChatResponse(
        String answer,
        double confidence,
        String source
) {
    public static ChatResponse of(String answer, double confidence, String source) {
        return new ChatResponse(answer, confidence, source);
    }
}
