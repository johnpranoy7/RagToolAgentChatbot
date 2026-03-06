package com.vfu.chatbot.service;

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

    public double evaluateConfidence(String question, String answer) {

        String judgePrompt = """
                Evaluate the confidence of the following AI answer.
                
                Question: %s
                
                Answer: %s
                
                Score confidence between 0.00 and 1.00 based on:
                - factual correctness
                - completeness
                - clarity
                
                Return ONLY a number between 0 and 1.
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
                return llmConfidence; // deterministic API

            default:
                double confidenceFromLlmAsJudge = this.evaluateConfidence(question, answer);
                log.info("Confidence from LLM is {}, confidenceFromLlmAsJudge:{}", llmConfidence, confidenceFromLlmAsJudge);
                return confidenceFromLlmAsJudge;
        }
    }

}