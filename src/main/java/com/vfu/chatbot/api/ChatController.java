package com.vfu.chatbot.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@Slf4j
public class ChatController {

    /** Below this judge score, tool-backed answers are not shown (hallucination guard). */
    private static final double JUDGE_PASS_THRESHOLD = 0.40;

    private static final String SOURCE_NEEDS_VERIFICATION = "NEEDS_VERIFICATION";
    private static final String SOURCE_AGENT_HANDOFF = "AGENT_HANDOFF";
    private static final String SOURCE_OUT_OF_SCOPE = "OUT_OF_SCOPE";
    private static final String SOURCE_RULE_FALLBACK = "RULE_FALLBACK";
    /** Answer withheld: judge confidence too low for evidence-backed response. */
    private static final String SOURCE_JUDGE_BLOCKED = "JUDGE_BLOCKED";

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
        Map<String, Object> sessionData = getSessionDataForLLMContext(finalSessionId, sessionId);

        String rawResponse = callandGetResponseFromLLM(chatRequest, finalSessionId, sessionData);
        log.info("rawResponse: {}", rawResponse);

        String answer = extractContent(rawResponse);
        double ruleConfidence = extractConfidence(rawResponse);
        String parsedSource = extractSource(rawResponse);

        String source = classifySource(parsedSource);

        //Calling Latest sessionData to verify against latest fetched data
        String evidenceForJudge = buildEvidenceContextForJudge(getSessionDataForLLMContext(finalSessionId, finalSessionId), source);
        Double judgeScore = null;
        if (requiresLlmJudge(source)) {
            judgeScore = confidenceService.judgeConfidence(
                    chatRequest.message(), answer, source, evidenceForJudge);
        }

        log.info("message from rule LLM: {} | source: {} | ruleConfidence: {} | judge: {}",
                answer, source, ruleConfidence, judgeScore != null ? judgeScore : "skipped");

        ChatResponse chatResponse = buildResponse(answer, source, ruleConfidence, judgeScore, sessionId);
        log.info("chatResponse: {}", chatResponse);
        chatAnalyticsService.logChat(sessionId, chatRequest.message(), chatResponse.content(), chatResponse.confidence(), chatResponse.source());

        return ResponseEntity.ok()
                .header("X-Session-ID", sessionId)
                .body(chatResponse);
    }

    /**
     * LLM judge runs only for reservation, property, and policy (RAG). All other sources use the model answer
     * and parsed {@code CONFIDENCE:} (with defaults when missing).
     */
    private ChatResponse buildResponse(
            String answer,
            String source,
            double ruleConfidence,
            Double judgeScore,
            String sessionId) {

        String s = source == null ? "" : source.trim().toUpperCase();

        if (SOURCE_AGENT_HANDOFF.equals(s)) {
            metricCounters.incrementAgentEscalations();
            double c = Math.max(effectiveRuleConfidence(ruleConfidence, 0.95), 0.95);
            return new ChatResponse(answer, c, SOURCE_AGENT_HANDOFF, sessionId);
        }

        if (SOURCE_NEEDS_VERIFICATION.equals(s)) {
            double c = Math.max(effectiveRuleConfidence(ruleConfidence, 0.85), 0.85);
            return new ChatResponse(answer, c, SOURCE_NEEDS_VERIFICATION, sessionId);
        }

        if (SOURCE_OUT_OF_SCOPE.equals(s)) {
            return new ChatResponse(answer, effectiveRuleConfidence(ruleConfidence, 0.50), SOURCE_OUT_OF_SCOPE, sessionId);
        }

        if ("GREETING".equals(s)) {
            double c = Math.max(effectiveRuleConfidence(ruleConfidence, 0.90), 0.90);
            return new ChatResponse(answer, c, "GREETING", sessionId);
        }

        if ("NONE".equals(s) || "UNKNOWN".equals(s) || SOURCE_RULE_FALLBACK.equals(s)) {
            metricCounters.incrementAgentEscalations();
            return new ChatResponse(answer, effectiveRuleConfidence(ruleConfidence, 0.35), SOURCE_RULE_FALLBACK, sessionId);
        }

        // Only reservation, property, RAG: judge + 40% gate (hallucination guard).
        if (requiresLlmJudge(s)) {
            double js = judgeScore != null ? judgeScore : 0.0;
            if (js < JUDGE_PASS_THRESHOLD) {
                metricCounters.incrementAgentEscalations();
                String safe = supportScopeMessage();
                return new ChatResponse(safe, js, SOURCE_JUDGE_BLOCKED, sessionId);
            }
            return new ChatResponse(answer, js, source, sessionId);
        }

        // e.g. GEOAPIFY_PLACES: no judge — show model response and rule confidence (default if unparsed).
        if ("GEOAPIFY_PLACES".equals(s)) {
            return new ChatResponse(answer, effectiveRuleConfidence(ruleConfidence, 0.88), source, sessionId);
        }

        return new ChatResponse(answer, effectiveRuleConfidence(ruleConfidence, 0.55), source, sessionId);
    }

    /** Parsed model confidence, or a default when the model omitted CONFIDENCE:. */
    private static double effectiveRuleConfidence(double parsed, double defaultWhenMissingOrTiny) {
        if (parsed > 0.05) {
            return Math.min(1.0, Math.max(0.0, parsed));
        }
        return defaultWhenMissingOrTiny;
    }

    private static boolean requiresLlmJudge(String normalizedSource) {
        return "RESERVATION".equals(normalizedSource)
                || "PROPERTY".equals(normalizedSource)
                || "POLICY_RAG".equals(normalizedSource);
    }

    /**
     * For POLICY_RAG, session does not contain retrieved chunks; the judge is told to evaluate honesty of
     * retrieval outcomes (including no-match) rather than verbatim policy text.
     */
    private String buildEvidenceContextForJudge(Map<String, Object> sessionData, String source) {
        String base = buildEvidenceContext(sessionData);
        if (source != null && "POLICY_RAG".equalsIgnoreCase(source.trim())) {
            return base + "; policyRagNote=Vector_retrieved_passages_are_not_persisted_in_session;"
                    + " evaluate whether the answer reflects an honest policy search outcome.";
        }
        return base;
    }

    private String supportScopeMessage() {
        String phone = customerSupportPhone != null ? customerSupportPhone : "";
        String email = customerSupportEmail != null ? customerSupportEmail : "";
        return "I can only help with reservation details, property information, rental policies, and nearby attractions. "
                + "For anything else, please contact Customer Service at " + phone + " / " + email + ".";
    }

    private @Nullable String callandGetResponseFromLLM(ChatRequest chatRequest, String finalSessionId, Map<String, Object> sessionData) {
        return chatClient.prompt().user(u -> u.text(chatRequest.message()))
                .system(s -> s.params(sessionData))
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

    private String extractContent(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return "";
        }
        try {
            Pattern metaStart = Pattern.compile("(?im)^\\s*(\\*\\*\\s*)?(CONFIDENCE|SOURCE)\\s*:\\s*(\\*\\*)?.*$");
            Matcher matcher = metaStart.matcher(rawResponse);
            String answerBlock = matcher.find() ? rawResponse.substring(0, matcher.start()) : rawResponse;
            return answerBlock
                    .trim()
                    .replaceAll("(?i)^\\s*(\\*\\*\\s*)?ANSWER\\s*:\\s*(\\*\\*)?", "")
                    .trim();
        } catch (RuntimeException e) {
            log.warn("extractContent failed, using raw text: {}", e.getMessage());
            return rawResponse.trim();
        }
    }

    /**
     * Reads the first CONFIDENCE: value in the raw assistant message (0–1 scale, optional %).
     */
    private double extractConfidence(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return 0.01;
        }
        try {
            // Allow: 0.92, .92, 92%, **CONFIDENCE:** 0.9
            Matcher relaxed = Pattern.compile(
                    "(?im)CONFIDENCE\\s*:\\s*(?:\\*\\*)?\\s*([0-9]*\\.?[0-9]+)\\s*(%)?"
            ).matcher(rawResponse);
            if (relaxed.find()) {
                String num = relaxed.group(1);
                boolean percent = relaxed.group(2) != null && !relaxed.group(2).isEmpty();
                double v = Double.parseDouble(num);
                if (percent) {
                    v = v / 100.0;
                }
                if (v > 1.0 && !percent) {
                    v = v / 100.0;
                }
                return Math.min(1.0, Math.max(0.0, v));
            }
            if (rawResponse.contains("6-digit") && rawResponse.contains("last name")) {
                return 0.85;
            }
        } catch (RuntimeException e) {
            log.error("extractConfidence parse error: {}", e.getMessage());
        }
        return 0.01;
    }

    private String extractSource(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return "unknown";
        }
        try {
            Pattern pattern = Pattern.compile("(?im)^\\s*(\\*\\*\\s*)?SOURCE\\s*:\\s*(\\*\\*)?\\s*([^\\r\\n]+?)\\s*$");
            Matcher matcher = pattern.matcher(rawResponse);
            if (matcher.find()) {
                return matcher.group(3).trim();
            }
        } catch (RuntimeException e) {
            log.warn("extractSource failed: {}", e.getMessage());
        }
        return "unknown";
    }

    /**
     * Source comes only from the assistant's declared SOURCE line (no extra LLM call).
     * Unknown or missing label maps to SOURCE_RULE_FALLBACK.
     */
    private String classifySource(String parsedSource) {
        String normalized = normalizeSourceLabel(parsedSource);
        if (normalized.equalsIgnoreCase("unknown") || normalized.isEmpty()) {
            return SOURCE_RULE_FALLBACK;
        }
        return normalized;
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
}
