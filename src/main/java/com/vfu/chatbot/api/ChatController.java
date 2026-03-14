package com.vfu.chatbot.api;

import com.vfu.chatbot.analytics.ChatAnalyticsService;
import com.vfu.chatbot.model.ChatRequest;
import com.vfu.chatbot.model.ChatResponse;
import com.vfu.chatbot.model.SessionEntity;
import com.vfu.chatbot.service.ConfidenceService;
import com.vfu.chatbot.service.SessionService;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@Slf4j
public class ChatController {

    private final ChatClient chatClient;
    private final ConfidenceService confidenceService;
    private final ChatAnalyticsService chatAnalyticsService;
    private final SessionService sessionService;

    public ChatController(ChatClient chatClient, ConfidenceService confidenceService, ChatAnalyticsService chatAnalyticsService, SessionService sessionService) {
        this.chatClient = chatClient;
        this.confidenceService = confidenceService;
        this.chatAnalyticsService = chatAnalyticsService;
        this.sessionService = sessionService;
    }

    @PostMapping("/chat")
    @RateLimiter(name = "rateLimitingApi", fallbackMethod = "chatRateLimited")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest chatRequest) {
        String sessionId = chatRequest.sessionId();
        log.info("sessionId: {}", sessionId);
        if (sessionId == null || sessionId.equalsIgnoreCase("unknown") || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }

        String finalSessionId = sessionId;
        log.info("finalsessionId: {}", finalSessionId);

        Optional<SessionEntity> sessionOpt = sessionService.getActiveSession(finalSessionId);
        Map<String, Object> sessionData = new HashMap<>(Map.of("sessionId", sessionId));
        sessionOpt.ifPresentOrElse(
                sessionEntity -> {
                    sessionData.put("isVerified", sessionEntity.isVerified());
                    sessionData.put("reservationId", sessionEntity.getReservationId()!=null?sessionEntity.getReservationId():"");
                    sessionData.put("lastName", sessionEntity.getLastName()!=null?sessionEntity.getLastName():"");
                    sessionData.put("unitId", sessionEntity.getUnitId()!=null?sessionEntity.getUnitId():"");
                    sessionData.put("latitude", sessionEntity.getLatitude()!=null?sessionEntity.getLatitude():"");
                    sessionData.put("longitude", sessionEntity.getLongitude()!=null?sessionEntity.getLongitude():"");
                },
                () -> {
                    sessionData.put("isVerified", false);
                    sessionData.put("reservationId", "");
                    sessionData.put("lastName", "");
                    sessionData.put("unitId", "");
                    sessionData.put("latitude", "");
                    sessionData.put("longitude", "");
                }
        );

        log.info("sessionData being passed to chatClient: {}", sessionData.values());

        String rawResponse = chatClient.prompt().user(u -> u.text(chatRequest.message()))
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, finalSessionId))
                .toolContext(sessionData)
                .call().content();

        // 2. PARSE CONFIDENCE & SOURCE (your exact format)
        String answer = extractContent(rawResponse);
        double ruleConfidence = extractConfidence(rawResponse);
        String source = extractSource(rawResponse);

        // For API using LLM Confidence, for RAG and General Chat using LLM-AS-Judge implementation
        double judgeConfidence = confidenceService.calculateConfidence(chatRequest.message(), answer, source, ruleConfidence);
        log.info("message from rule LLM :{} and source:{}", answer, source);
        log.info("Confidence in Controller from rule is {}, judge:{}", ruleConfidence, judgeConfidence);
        ChatResponse chatResponse;

        chatAnalyticsService.logChat(sessionId, chatRequest.message(), answer, ruleConfidence >= 0.8 ? ruleConfidence : judgeConfidence,
                source);

        if (source.equalsIgnoreCase("NONE") || source.equalsIgnoreCase("unknown")) {
            chatResponse = new ChatResponse(answer, ruleConfidence, "RULE_FALLBACK", sessionId);
        } else if (ruleConfidence >= 0.8) {
            // Tool-backed → Trust rules, deliver
            chatResponse = new ChatResponse(answer, ruleConfidence, source, sessionId);
        } else if (judgeConfidence >= 0.4) {
            // Judge confident enough → Deliver
            chatResponse = new ChatResponse(answer, judgeConfidence, source, sessionId);
        } else {
            // Both low → Agent escalation
            return agentEscalation(sessionId);
        }

        return ResponseEntity.ok()
                .header("X-Session-ID", sessionId)  // Always return
                .body(chatResponse);

    }

    @GetMapping("dashboard")
    public Map<String, Object> chat() {
        return chatAnalyticsService.getDashboardStats();
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

    private ResponseEntity<ChatResponse> agentEscalation(String sessionId) {
        return ResponseEntity.ok()
                .body(new ChatResponse(
                        "I am not able to help here. Could you please connect with Customer Support Agent",
                        0.0,
                        "AGENT_ESCALATION",
                        sessionId
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
        double confidence = 0.01;
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException e) {
                log.error("Error extracting confidence", e);
                confidence = 0.01;
            }
        }
        // 2. SAFETY NET: Perfect contextual responses
        if (rawResponse.contains("6-digit") && rawResponse.contains("last name")) {
            confidence = 0.85;  // Perfect "ask for reservation" response
        }
        return confidence;
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
