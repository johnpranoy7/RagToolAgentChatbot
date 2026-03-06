package com.vfu.chatbot.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class ControllerExceptionHandler {
    @ExceptionHandler(AiToolException.class)
    public ResponseEntity<Map<String, Object>> handleAiToolException(
            AiToolException ex,
            WebRequest webRequest) {

        // Extract context for logging
        String sessionId = extractSessionId(webRequest);
        String path = webRequest.getDescription(false);
        String traceId = UUID.randomUUID().toString().substring(0, 8);

        // Full error logging with context
        log.error("AI TOOL EXCEPTION - Session:'{}', TraceId:'{}', Path:'{}', Message:'{}'",
                sessionId, traceId, path, ex.getMessage(), ex);

        // Structured error response
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("timestamp", LocalDateTime.now().toString());
        error.put("error", "TOOL_ERROR");
        error.put("message", ex.getMessage());
        error.put("sessionId", sessionId);
        error.put("traceId", traceId);
        error.put("path", path);

        return ResponseEntity.badRequest()
                .header("X-Trace-ID", traceId)
                .header("X-Session-ID", sessionId)
                .body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex,
            WebRequest webRequest) {

        String sessionId = extractSessionId(webRequest);
        String path = webRequest.getDescription(false);
        String traceId = UUID.randomUUID().toString().substring(0, 8);

        log.error("UNHANDLED EXCEPTION - Session:'{}', TraceId:'{}', Path:'{}', Type:'{}'",
                sessionId, traceId, path, ex.getClass().getSimpleName(), ex);

        Map<String, Object> error = new LinkedHashMap<>();
        error.put("timestamp", LocalDateTime.now().toString());
        error.put("error", "INTERNAL_ERROR");
        error.put("message", "An unexpected error occurred");
        error.put("sessionId", sessionId);
        error.put("traceId", traceId);
        error.put("path", path);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("X-Trace-ID", traceId)
                .header("X-Session-ID", sessionId)
                .body(error);
    }

    private String extractSessionId(WebRequest webRequest) {
        String sessionId = webRequest.getHeader("X-Session-ID");
        return sessionId != null ? sessionId : "unknown";
    }
}
