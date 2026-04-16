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
    public double evaluateConfidence(String question, String answer, String source, String evidenceContext) {

        String judgePrompt = """
                    You are a strict factual judge for a vacation rental chatbot.
                    Score confidence from 0.00-1.00.
                    
                    User Question: %s
                    Assistant Answer: %s
                    Source Type: %s
                    Available Evidence: %s
                    
                    Scoring instructions:
                    - Compare answer claims against Available Evidence.
                    - Available Evidence includes verified session attributes and cached reservation/property summaries.
                    - If the answer contains a claim that is absent, unknown, or contradicted by evidence, lower score strongly.
                    - If the answer explicitly states missing information when evidence is unknown, reward that behavior.
                    - Penalize over-confident language when evidence is partial.
                    - Reward concise answers that stay in scope and grounded.
                    - Return ONLY one decimal number between 0.00 and 1.00.
                    
                    Examples:
                    - Answer includes "max children" but evidence has only max_adults/max_occupants and no max_children: <=0.35
                    - Answer correctly says "max children is not available in our data": >=0.80
                    
                    Return only the numeric score.
                    Question: %s
                    Answer: %s
                """.formatted(question, answer, source, evidenceContext, question, answer);

        String response = chatClient.prompt()
                .user(judgePrompt)
                .call()
                .content();

        try {
            return clampConfidence(Double.parseDouble(response.trim()));
        } catch (Exception e) {
            return 0.5;
        }
    }


    public double calculateConfidence(String question,
                                      String answer,
                                      String source,
                                      double llmConfidence,
                                      String evidenceContext) {
        if (source == null) {
            return clampConfidence(evaluateConfidence(question, answer, "UNKNOWN", evidenceContext));
        }
        switch (source) {

            case "GREETING":
            case "NEEDS_VERIFICATION":
            case "AGENT_HANDOFF":
                return clampConfidence(Math.max(llmConfidence, evaluateConfidence(question, answer, source, evidenceContext)));
            case "NONE":
            case "OUT_OF_SCOPE":
                return 0.0;
            case "RESERVATION":
            case "PROPERTY":
            case "POLICY_RAG":
            case "GEOAPIFY_PLACES":
                return clampConfidence(Math.min(llmConfidence, evaluateConfidence(question, answer, source, evidenceContext)));
            default:
                double confidenceFromLlmAsJudge = this.evaluateConfidence(question, answer, source, evidenceContext);
                log.info("Confidence from LLM is {}, confidenceFromLlmAsJudge:{}", llmConfidence, confidenceFromLlmAsJudge);
                return clampConfidence(confidenceFromLlmAsJudge);
        }
    }

    @Timed(value = "chatbot.source.adjudicate", description = "LLM source adjudication")
    public String adjudicateSource(String question, String answer, String parsedSource, String evidenceContext) {
        String sourcePrompt = """
                You are a strict classifier for chatbot response sources.
                Choose exactly one label from:
                GREETING, POLICY_RAG, RESERVATION, PROPERTY, GEOAPIFY_PLACES, NEEDS_VERIFICATION, AGENT_HANDOFF, OUT_OF_SCOPE, NONE.

                User Question: %s
                Assistant Answer: %s
                Parsed Source From Assistant: %s
                Verified Session Evidence: %s

                Rules:
                - Use OUT_OF_SCOPE only for questions outside reservation/property/policy/nearby-places support.
                - Use NONE when question is in-scope but answer has insufficient trustworthy evidence.
                - Use NEEDS_VERIFICATION only if asking for confirmation id and last name.
                - Use AGENT_HANDOFF only when explicitly handing to a human/support agent.
                - Prefer RESERVATION/PROPERTY/POLICY_RAG/GEOAPIFY_PLACES when answer is grounded in those domains.

                Return only one label.
                """.formatted(question, answer, parsedSource, evidenceContext);

        String response = chatClient.prompt()
                .user(sourcePrompt)
                .call()
                .content();

        if (response == null || response.isBlank()) {
            return "unknown";
        }
        return response.trim().toUpperCase().replace(' ', '_');
    }

    private double clampConfidence(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }

}