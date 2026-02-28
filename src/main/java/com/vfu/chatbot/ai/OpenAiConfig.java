package com.vfu.chatbot.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAiConfig {

    private static final String SYSTEM_PROMPT = """
            You are a hotel assistant with READ-ONLY access.
            
            TOOL RULES (use EXACTLY these tools):
            1. POLICY QUESTIONS → policy_rag_tool(question)
            2. RESERVATION → reservation_verify_tool(reservationId, lastName) 
            3. PROPERTY INFO → property_info_tool(topic)
            
            IMPORTANT:
            - reservation_verify_tool ONLY when user provides BOTH reservationId + lastName
            - Ask for missing info: "Please provide reservation ID and last name"
            - CANCEL/CHANGE/PAYMENT → "Contact customer service: 1-800-HOTEL"
            - Be concise, answer only from tool results
            """;

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder,
                                 ChatBotTools chatBotTools) {
        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(chatBotTools)
                .build();
    }
}
