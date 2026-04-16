package com.vfu.chatbot.api;

import com.vfu.chatbot.analytics.ChatAnalyticsService;
import com.vfu.chatbot.model.ChatRequest;
import com.vfu.chatbot.model.ChatResponse;
import com.vfu.chatbot.model.SessionEntity;
import com.vfu.chatbot.monitoring.MetricCounters;
import com.vfu.chatbot.service.ConfidenceService;
import com.vfu.chatbot.service.SessionService;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Value;
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
    private static final String SOURCE_NEEDS_VERIFICATION = "NEEDS_VERIFICATION";
    private static final String SOURCE_AGENT_HANDOFF = "AGENT_HANDOFF";
    private static final String SOURCE_OUT_OF_SCOPE = "OUT_OF_SCOPE";
    private static final String SOURCE_RULE_FALLBACK = "RULE_FALLBACK";

    @Value("${app.vfu.customerSupport.phone:}")
    private String customerSupportPhone;

    @Value("${app.vfu.customerSupport.email:}")
    private String customerSupportEmail;

    private final ChatClient chatClient;
    private final ConfidenceService confidenceService;
    private final ChatAnalyticsService chatAnalyticsService;
    private final SessionService sessionService;
    private final MetricCounters metricCounters;

    public ChatController(ChatClient chatClient, ConfidenceService confidenceService, ChatAnalyticsService chatAnalyticsService, SessionService sessionService, MetricCounters metricCounters) {
        this.chatClient = chatClient;
        this.confidenceService = confidenceService;
        this.chatAnalyticsService = chatAnalyticsService;
        this.sessionService = sessionService;
        this.metricCounters = metricCounters;
    }

    @PostMapping("/chat")
    @RateLimiter(name = "rateLimitingApi", fallbackMethod = "chatRateLimited")
    @Timed(
            value = "chatbot.chat.endpoint",
            description = "End-to-end /chat endpoint latency",
            percentiles = {0.5, 0.95, 0.99},
            histogram = true
    )
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest chatRequest) {
        String sessionId = chatRequest.sessionId();
        log.info("sessionId: {}", sessionId);
        if (sessionId == null || sessionId.equalsIgnoreCase("unknown") || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }

        String finalSessionId = sessionId;
        log.info("finalsessionId: {}", finalSessionId);

        Map<String, Object> sessionData = getSessionDataForLLMContext(finalSessionId, sessionId);

        String rawResponse = callandGetResponseFromLLM(chatRequest, finalSessionId, sessionData);

        // 2. PARSE CONFIDENCE & SOURCE (your exact format)
        String answer = extractContent(rawResponse);
        double ruleConfidence = extractConfidence(rawResponse);
        String parsedSource = extractSource(rawResponse);
        String evidenceContext = buildEvidenceContext(sessionData);
        String source = classifySource(parsedSource, chatRequest.message(), answer, evidenceContext);

        // For API using LLM Confidence, for RAG and General Chat using LLM-AS-Judge implementation
        double judgeConfidence = confidenceService.calculateConfidence(
                chatRequest.message(), answer, source, ruleConfidence, evidenceContext);
        log.info("message from rule LLM :{} and source:{}", answer, source);
        log.info("Confidence in Controller from rule is {}, judge:{}", ruleConfidence, judgeConfidence);
        ChatResponse chatResponse;
        double finalConfidenceForAnalytics = chooseFinalConfidence(ruleConfidence, judgeConfidence, source);
        chatAnalyticsService.logChat(sessionId, chatRequest.message(), answer, finalConfidenceForAnalytics,
                source);

        if (source.equalsIgnoreCase(SOURCE_AGENT_HANDOFF)) {
            metricCounters.incrementAgentEscalations();
            chatResponse = new ChatResponse(answer, Math.max(judgeConfidence, 0.95), SOURCE_AGENT_HANDOFF, sessionId);
        } else if (source.equalsIgnoreCase(SOURCE_NEEDS_VERIFICATION)) {
            chatResponse = new ChatResponse(answer, Math.max(judgeConfidence, 0.85), SOURCE_NEEDS_VERIFICATION, sessionId);
        } else if (source.equalsIgnoreCase(SOURCE_OUT_OF_SCOPE)) {
            chatResponse = new ChatResponse(answer, Math.min(ruleConfidence, judgeConfidence), SOURCE_OUT_OF_SCOPE, sessionId);
        } else if (source.equalsIgnoreCase("NONE") || source.equalsIgnoreCase("unknown") || source.equalsIgnoreCase(SOURCE_RULE_FALLBACK)) {
            metricCounters.incrementAgentEscalations();
            log.info("Rule fallBack response :{}", answer);
            chatResponse = new ChatResponse(answer, Math.min(ruleConfidence, judgeConfidence), SOURCE_RULE_FALLBACK, sessionId);
        } else if (isTrustedToolSource(source) && ruleConfidence >= 0.75) {
            // Do not suppress valid tool-backed answers due to judge under-scoring.
            double stabilizedConfidence = Math.min(ruleConfidence, Math.max(judgeConfidence, 0.45));
            chatResponse = new ChatResponse(answer, stabilizedConfidence, source, sessionId);
        } else if (judgeConfidence >= 0.4) {
            chatResponse = new ChatResponse(answer, chooseFinalConfidence(ruleConfidence, judgeConfidence, source), source, sessionId);
        } else {
            // Both low → Agent escalation
            metricCounters.incrementAgentEscalations();
            return agentEscalation(sessionId);
        }

        return ResponseEntity.ok()
                .header("X-Session-ID", sessionId)  // Always return
                .body(chatResponse);

    }

    private @Nullable String callandGetResponseFromLLM(ChatRequest chatRequest, String finalSessionId, Map<String, Object> sessionData) {
        return chatClient.prompt().user(u -> u.text(chatRequest.message()))
                .system( s -> s.params(sessionData))
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, finalSessionId))
                .toolContext(sessionData)
                .call().content();
    }

    private @NonNull Map<String, Object> getSessionDataForLLMContext(String finalSessionId, String sessionId) {
        Optional<SessionEntity> sessionOpt = sessionService.getActiveSession(finalSessionId);
        Map<String, Object> sessionData = new HashMap<>(Map.of("sessionId", sessionId));
        sessionOpt.ifPresentOrElse(
                sessionEntity -> {
                    sessionData.put("isVerified", sessionEntity.isVerified());
                    sessionData.put("reservationId", sessionEntity.getReservationId() != null ? sessionEntity.getReservationId() : "");
                    sessionData.put("lastName", sessionEntity.getLastName() != null ? sessionEntity.getLastName() : "");
                    sessionData.put("unitId", sessionEntity.getUnitId() != null ? sessionEntity.getUnitId() : "");
                    sessionData.put("latitude", sessionEntity.getLatitude() != null ? sessionEntity.getLatitude() : "");
                    sessionData.put("longitude", sessionEntity.getLongitude() != null ? sessionEntity.getLongitude() : "");
                    sessionData.put("reservationFacts", sessionEntity.getCachedReservationSummary() != null ? sessionEntity.getCachedReservationSummary() : "");
                    sessionData.put("propertyFacts", sessionEntity.getCachedPropertySummary() != null ? sessionEntity.getCachedPropertySummary() : "");
                    sessionData.put("customerSupportPhone", customerSupportPhone != null ? customerSupportPhone : "");
                    sessionData.put("customerSupportEmail", customerSupportEmail != null ? customerSupportEmail : "");
                },
                () -> {
                    sessionData.put("isVerified", false);
                    sessionData.put("reservationId", "");
                    sessionData.put("lastName", "");
                    sessionData.put("unitId", "");
                    sessionData.put("latitude", "");
                    sessionData.put("longitude", "");
                    sessionData.put("reservationFacts", "");
                    sessionData.put("propertyFacts", "");
                    sessionData.put("customerSupportPhone", customerSupportPhone != null ? customerSupportPhone : "");
                    sessionData.put("customerSupportEmail", customerSupportEmail != null ? customerSupportEmail : "");
                }
        );

        log.info("sessionData being passed to chatClient: {}", sessionData.values());
        return sessionData;
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
        log.info("agentEscalation for sessionId: {}", sessionId);
        return ResponseEntity.ok()
                .body(new ChatResponse(
                        "I am not able to help here. Could you please connect with Customer Support Agent",
                        0.0,
                        "AGENT_ESCALATION",
                        sessionId
                ));
    }


    private String extractContent(String rawResponse) {
        // Accept both markdown and plain labels, e.g. "**CONFIDENCE:**" or "CONFIDENCE:"
        Pattern metaStart = Pattern.compile("(?im)^\\s*(\\*\\*\\s*)?(CONFIDENCE|SOURCE)\\s*:\\s*(\\*\\*)?.*$");
        Matcher matcher = metaStart.matcher(rawResponse);
        String answerBlock = matcher.find() ? rawResponse.substring(0, matcher.start()) : rawResponse;
        return answerBlock
                .trim()
                .replaceAll("(?i)^\\s*(\\*\\*\\s*)?ANSWER\\s*:\\s*(\\*\\*)?", "")
                .trim();
    }

    private double extractConfidence(String rawResponse) {
        Pattern pattern = Pattern.compile("(?im)^\\s*(\\*\\*\\s*)?CONFIDENCE\\s*:\\s*(\\*\\*)?\\s*([0-9](?:\\.[0-9]{1,2})?)\\s*$");
        Matcher matcher = pattern.matcher(rawResponse);
        double confidence = 0.01;
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(3));
            } catch (NumberFormatException e) {
                log.error("Error extracting confidence", e);
                // fall through to default below
            }
        }
        // 2. SAFETY NET: Perfect contextual responses
        if (rawResponse.contains("6-digit") && rawResponse.contains("last name")) {
            return 0.85;  // Perfect "ask for reservation" response
        }
        return confidence;
    }

    private String extractSource(String rawResponse) {
        Pattern pattern = Pattern.compile("(?im)^\\s*(\\*\\*\\s*)?SOURCE\\s*:\\s*(\\*\\*)?\\s*([^\\r\\n]+)\\s*$");
        Matcher matcher = pattern.matcher(rawResponse);
        if (matcher.find()) {
            return matcher.group(3).trim();
        }
        return "unknown";
    }

    private String classifySource(String parsedSource, String userMessage, String answer, String evidenceContext) {
        String normalizedParsedSource = normalizeSourceLabel(parsedSource);
        if (!normalizedParsedSource.equalsIgnoreCase("NONE")
                && !normalizedParsedSource.equalsIgnoreCase("unknown")
                && !normalizedParsedSource.equalsIgnoreCase(SOURCE_RULE_FALLBACK)) {
            return normalizedParsedSource;
        }

        String adjudicatedSource = confidenceService.adjudicateSource(
                userMessage, answer, normalizedParsedSource, evidenceContext);
        String normalizedAdjudicatedSource = normalizeSourceLabel(adjudicatedSource);
        if (normalizedAdjudicatedSource.equalsIgnoreCase("unknown")) {
            return SOURCE_RULE_FALLBACK;
        }
        return normalizedAdjudicatedSource;
    }

    private String normalizeSourceLabel(String source) {
        if (source == null) {
            return "unknown";
        }
        String normalized = source.trim().toUpperCase().replace(' ', '_');
        if (normalized.equals("POLICYRAG")) {
            return "POLICY_RAG";
        }
        if (normalized.equals("AGENT_ESCALATION")) {
            return SOURCE_AGENT_HANDOFF;
        }
        return normalized;
    }

    private String buildEvidenceContext(Map<String, Object> sessionData) {
        return "isVerified=" + String.valueOf(sessionData.getOrDefault("isVerified", false)) +
                "; reservationId=" + String.valueOf(sessionData.getOrDefault("reservationId", "")) +
                "; lastName=" + String.valueOf(sessionData.getOrDefault("lastName", "")) +
                "; unitId=" + String.valueOf(sessionData.getOrDefault("unitId", "")) +
                "; latitude=" + String.valueOf(sessionData.getOrDefault("latitude", "")) +
                "; longitude=" + String.valueOf(sessionData.getOrDefault("longitude", "")) +
                "; reservationFacts=" + String.valueOf(sessionData.getOrDefault("reservationFacts", "")) +
                "; propertyFacts=" + String.valueOf(sessionData.getOrDefault("propertyFacts", ""));
    }

    private double chooseFinalConfidence(double modelReportedConfidence, double judgeConfidence, String source) {
        if (source == null) {
            return judgeConfidence;
        }
        String normalizedSource = source.trim().toUpperCase();
        if (normalizedSource.equals("RESERVATION")
                || normalizedSource.equals("PROPERTY")
                || normalizedSource.equals("POLICY_RAG")
                || normalizedSource.equals("GEOAPIFY_PLACES")) {
            // Conservative aggregation: protects against hallucinated confidence inflation.
            return Math.min(modelReportedConfidence, judgeConfidence);
        }
        return judgeConfidence;
    }

    private boolean isTrustedToolSource(String source) {
        if (source == null) {
            return false;
        }
        String normalizedSource = source.trim().toUpperCase();
        return normalizedSource.equals("RESERVATION")
                || normalizedSource.equals("PROPERTY")
                || normalizedSource.equals("POLICY_RAG")
                || normalizedSource.equals("GEOAPIFY_PLACES");
    }

}
