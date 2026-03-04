package com.vfu.chatbot.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ControllerExceptionHandler {

    @ExceptionHandler(AiToolException.class)
    public ResponseEntity<Map<String, String>> handleAiToolException(AiToolException ex) {
        Map<String, String> error = Map.of(
                "error", "TOOL_ERROR",
                "message", ex.getMessage()
        );
        return ResponseEntity.badRequest().body(error);
    }
}

