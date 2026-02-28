package com.vfu.chatbot.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class ChatBotTools {

    private final PolicyRagService policyRagService;
    private final PropertyInfoService propertyInfoService;
    private final ReservationVerifyService reservationVerifyService;

    public ChatBotTools(PolicyRagService policyRagService, PropertyInfoService propertyInfoService, ReservationVerifyService reservationVerifyService) {
        this.policyRagService = policyRagService;
        this.propertyInfoService = propertyInfoService;
        this.reservationVerifyService = reservationVerifyService;
    }

    @Tool(description = "Search hotel policies and rules by question")
    public String policy_rag_tool(
            @ToolParam(description = "Policy question to search") String question) {

        log.info("Policy RAG search: {}", question);
        List<String> chunks = policyRagService.searchPolicyChunks(question);
        return String.join("\n", chunks);
    }

    @Tool(description = "Get hotel property information by topic (wifi, parking, checkin time, etc.)")
    public String property_info_tool(
            @ToolParam(description = "Topic to search (wifi, parking, checkin, checkout)") String topic) {

        log.info("Property info requested for: {}", topic);
        return propertyInfoService.getPropertyInfo(topic);
    }

    @Tool(description = """
            Verifies a hotel reservation ownership and returns reservation details.
            Use ONLY when user provides BOTH reservationId and lastName.
            Returns structured JSON that you can use to answer reservation questions.
            """)
    public ReservationResult reservation_verify_tool(
            @ToolParam(description = "The 5-digit reservation ID") String reservationId,
            @ToolParam(description = "Guest's last name as on booking") String lastName) {

        log.info("Verifying reservation: {} - {}", reservationId, lastName);

        // Use your stub service (replace with real API later)
        boolean verified = reservationVerifyService.verifyReservation(reservationId, lastName);

        if (verified) {
            return new ReservationResult(
                    true,
                    reservationId,
                    "John " + lastName,
                    "2026-03-01",
                    "2026-03-03",
                    "Deluxe King",
                    "CONFIRMED",
                    "Late checkout requested",
                    "Grand Hotel Atlanta",
                    "123 Peachtree St, Atlanta GA",
                    "(404) 555-0123",
                    "3:00 PM",
                    "11:00 AM"
            );
        } else {
            return ReservationResult.notFound(reservationId);
        }
    }

    public record ReservationResult(
            boolean found,
            String reservationId,
            String guestName,
            String checkInDate,
            String checkOutDate,
            String roomType,
            String status,
            String specialRequests,
            String propertyName,
            String propertyAddress,
            String propertyPhone,
            String checkInTime,
            String checkOutTime
    ) {
        public static ReservationResult notFound(String reservationId) {
            return new ReservationResult(
                    false, reservationId, null, null, null, null, null, null,
                    null, null, null, null, null
            );
        }
    }

}
