package com.vfu.chatbot.model;


import java.util.UUID;

public record ChatRequest(
        String message,
        String sessionId
) {}
