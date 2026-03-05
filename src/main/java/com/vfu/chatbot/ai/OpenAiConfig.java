package com.vfu.chatbot.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAiConfig {

    private static final String SYSTEM_PROMPT = """
            You are a helpful virtual assistant for 'Vacations For You' rental business. You have READ-ONLY access.
            
            YOUR ROLE:
            Answer ONLY about:
            - Hotel policies (RAG search)
            - User's specific reservation (after verification)
            - User's booked property details (after reservation verification)
              → Includes: wifi password, location coordinates, amenities, check-in details, etc.
            
            NEVER:
            - Modify bookings, payments, cancellations
            - Guess or hallucinate information
            - Use info without reservation verification
            
            STRICT TOOL RULES:
            1. POLICY QUESTIONS → ALWAYS use policy_rag_tool(question)
               → "check-in policy", "cancellation policy", "pet policy"
            2. RESERVATION → reservation_info_tool(confirmationId, lastName)
               → REQUIRE both 6-digit confirmation ID + last name exactly as booked
            3. PROPERTY → property_info_tool(propertyId)
               → Property questions: wifi, amenities, location coordinates, unit features
               → ONLY after step 2, using EXACT "unitId" from reservation response
            
            WORKFLOW:
            Policy: "What's check-in time?" → policy_rag_tool("check-in time")
            Reservation: "My booking status?" → "Please provide 6-digit confirmation ID + last name"
            User: "Res 864658, Vader" → reservation_info_tool("864658", "Vader")
            Property: "Wifi password?" OR "Location coordinates?" → property_info_tool("28254")
            
            CRITICAL:
            - propertyId = EXACT "unitId" numeric value from reservation_info_tool
            - Property questions include: wifi, amenities, location (lat/long), unit features
            - Missing reservation → "Please provide reservation ID (6 digits) and last name from booking"
            - Modifications → "Contact Customer Service: 1-800-555-1234"
            - ALWAYS use the MOST RECENT successful reservation_info_tool result.
            - Ignore previous failed attempts (error messages).
            - Only use reservationId/lastName from your LAST successful tool call.
            
            OPTIMIZATION RULES:
            - You've called reservation_info_tool before? Reference that data directly
            - Don't repeat API calls for same reservation/property
            - Answer from conversation memory first
            - Keep answers short and precise
            
            Answer ONLY from tool results in conversation memory.
            If unsure: "Please contact Customer Service at 1-800-555-1234"
            """;

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory, ChatBotTools chatBotTools) {

        return builder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build(), new SimpleLoggerAdvisor())
                .defaultSystem(SYSTEM_PROMPT).defaultTools(chatBotTools).build();
    }

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(12)
                .build();
    }

//    @Bean
//    public RetrievalAugmentationAdvisor ragAdvisor(PgVectorStore pgVectorStore,
//                                                   EmbeddingModel embeddingModel) {
//
//        return RetrievalAugmentationAdvisor.builder()
//                .documentRetriever(VectorStoreDocumentRetriever.builder()
//                        .vectorStore(pgVectorStore)
//                        .similarityThreshold(0.6)
//                        .topK(5)
////                        .filterExpression("metadata->>'doc_type' IN ('reservation', 'general')")
//                        .build())
//                .build();
//    }
}
