package com.vfu.chatbot.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ConfidenceService {

    private static final Pattern FIRST_DECIMAL = Pattern.compile("([01](?:\\.\\d+)?)");

    private final ChatClient chatClient;

    public ConfidenceService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    /**
     * LLM-as-judge: intended to be called only for {@code RESERVATION}, {@code PROPERTY}, and {@code POLICY_RAG}.
     * Score is the user-facing confidence for those sources.
     * Retrieved policy text is not in session evidence; the prompt instructs the judge to score honest
     * no-match / support-contact answers and to penalize invented policy details.
     */
    @Timed(value = "chatbot.confidence.judge", description = "LLM Judge confidence scoring")
    public double judgeConfidence(String question, String answer, String source, String evidenceContext) {
        String sourceNote = buildSourceSpecificJudgeNote(source);

        String judgePrompt = """
                You are a strict factual judge for a vacation rental chatbot.
                Score how well the assistant answer is supported by the evidence, from 0.00 to 1.00.

                User Question: %s
                Assistant Answer: %s
                Declared Source Type: %s
                Available Evidence (session + summaries): %s
                %s
                Rules:
                - Compare concrete claims in the answer against the evidence.
                - If the answer invents facts, contradicts evidence, or over-claims, score low (below 0.40).
                - If the answer admits missing data or honestly says no matching policy was found and gives support contact, score high when appropriate.
                - Policy/RAG: retrieved chunks are NOT included in Available Evidence below; score whether the answer is an honest retrieval outcome (including no match + rephrase + support contact), not whether full policy text appears in evidence.
                - Reservation/Property: dates may be phrased differently than RESERVATION_FACTS/PROPERTY_FACTS (e.g. long-form dates); treat matching facts as supported.
                - Return ONLY one number between 0.00 and 1.00, no other text if possible.

                Question: %s
                Answer: %s
                """.formatted(question, answer, source, evidenceContext, sourceNote, question, answer);

        String response = chatClient.prompt()
                .user(judgePrompt)
                .call()
                .content();

        return clampConfidence(parseJudgeNumber(response));
    }

    private static String buildSourceSpecificJudgeNote(String source) {
        if (source == null) {
            return "";
        }
        return switch (source.trim().toUpperCase().replace(' ', '_')) {
            case "POLICY_RAG" -> """
                    
                    POLICY_RAG: You do not see the actual vector-store passages. Trust an answer that clearly states no matching passage was found, suggests rephrasing, and gives support contact (0.75–0.90). Penalize specific policy claims that sound invented.
                    """;
            case "RESERVATION", "PROPERTY" -> """
                    
                    RESERVATION/PROPERTY: Match answer facts to RESERVATION_FACTS / PROPERTY_FACTS; ignore formatting (bullets, long-form dates).
                    """;
            default -> "";
        };
    }

    private static double parseJudgeNumber(String response) {
        if (response == null || response.isBlank()) {
            return 0.5;
        }
        String trimmed = response.trim();
        try {
            return Double.parseDouble(trimmed);
        } catch (NumberFormatException ignored) {
            Matcher m = FIRST_DECIMAL.matcher(trimmed);
            if (m.find()) {
                try {
                    return Double.parseDouble(m.group(1));
                } catch (NumberFormatException ignored2) {
                    // fall through
                }
            }
            return 0.5;
        }
    }

    private static double clampConfidence(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }
}
