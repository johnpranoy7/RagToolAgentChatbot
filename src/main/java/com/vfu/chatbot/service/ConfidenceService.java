package com.vfu.chatbot.service;

import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ConfidenceService {

    private final ChatClient chatClient;


    public ConfidenceService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @Timed(value = "chatbot.confidence.calculate", description = "LLM Judge confidence scoring")
    public double evaluateConfidence(String question, String answer) {

        String judgePrompt = """
                    Rate ONLY factual correctness (0.00-1.00) for this vacation rental chatbot.
                    Question: %s
                    Answer: %s
                    Rules:
                    Score confidence between 0.00 and 1.00 based on:
                     - factual correctness
                     - completeness
                     - clarity
                    Return ONLY number (no text).
                """.formatted(question, answer);

        String response = chatClient.prompt()
                .user(judgePrompt)
                .call()
                .content();

        try {
            return Double.parseDouble(response.trim());
        } catch (Exception e) {
            return 0.5;
        }
    }


    public double calculateConfidence(String question,
                                      String answer,
                                      String source, double llmConfidence) {
        switch (source) {

            case "RESERVATION":
            case "PROPERTY":
            case "GREETING":
            case "NEEDS_VERIFICATION":
            case "AGENT_HANDOFF":
                return llmConfidence; // deterministic API
            case "NONE":
            case "OUT_OF_SCOPE":
                return 0.0;
            default:
                double confidenceFromLlmAsJudge = this.evaluateConfidence(question, answer);
                log.info("Confidence from LLM is {}, confidenceFromLlmAsJudge:{}", llmConfidence, confidenceFromLlmAsJudge);
                return confidenceFromLlmAsJudge;
        }
    }

}