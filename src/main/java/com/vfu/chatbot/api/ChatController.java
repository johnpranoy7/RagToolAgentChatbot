package com.vfu.chatbot.api;

import com.vfu.chatbot.model.ChatRequest;
import com.vfu.chatbot.model.ChatResponse;
import com.vfu.chatbot.service.ConfidenceService;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
@Slf4j
public class ChatController {

    private final ChatClient chatClient;
    private final ConfidenceService confidenceService;

    public ChatController(ChatClient chatClient, ConfidenceService confidenceService) {
        this.chatClient = chatClient;
        this.confidenceService = confidenceService;
    }

    @PostMapping
    @RateLimiter(name = "rateLimitingApi", fallbackMethod = "chatRateLimited")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest chatRequest) {
        String sessionId = chatRequest.sessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }

        String finalSessionId = sessionId;
        String rawResponse = chatClient.prompt().user(u -> u.text(chatRequest.message()))
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, finalSessionId))
                .toolContext(Map.of("sessionId", sessionId))
                .call().content();

        // 2. PARSE CONFIDENCE & SOURCE (your exact format)
        String answer = extractContent(rawResponse);
        double confidenceFromLLM = extractConfidence(rawResponse);
        String source = extractSource(rawResponse);

        // For API using LLM Confidence, for RAG and General Chat using LLM-AS-Judge implementation
        double confidence = confidenceService.calculateConfidence(chatRequest.message(), answer, source, confidenceFromLLM);

        ChatResponse chatResponse = new ChatResponse(answer, confidence, source, sessionId);

        return ResponseEntity.ok()
                .header("X-Session-ID", sessionId)  // Always return
                .body(chatResponse);

    }

    public ResponseEntity<ChatResponse> chatRateLimited(
            @RequestBody ChatRequest chatRequest, RequestNotPermitted ex) {
        log.warn("Rate limit exceeded for session: {}, :{}", chatRequest.sessionId(), ex.getMessage());

        return ResponseEntity.status(429)
                .header("Retry-After", "60")
                .body(new ChatResponse(
                        "Too many messages. Please wait 1 minute (20 msg/min limit).",
                        0.0, "rate-limit", chatRequest.sessionId()
                ));
    }


    private String extractContent(String rawResponse) {
        // Everything before **CONFIDENCE:**
        String[] parts = rawResponse.split("\\*\\*CONFIDENCE:", 2);
        if (parts.length > 0) {
            return parts[0].trim().replaceAll("^\\*\\*ANSWER:\\*\\*", "").trim();
        }
        return rawResponse;
    }

    private double extractConfidence(String rawResponse) {
        Pattern pattern = Pattern.compile("(?i)\\*\\*CONFIDENCE:\\*\\*\\s([0-9]\\.[0-9]{2})");
        Matcher matcher = pattern.matcher(rawResponse);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException e) {
                log.error("Error extracting confidence", e);
                return 0.01;
            }
        }
        return 0.01;
    }

    private String extractSource(String rawResponse) {
        Pattern pattern = Pattern.compile("(?i)\\*\\*SOURCE:\\*\\*([^\\n]+)");
        Matcher matcher = pattern.matcher(rawResponse);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "unknown";
    }

}
