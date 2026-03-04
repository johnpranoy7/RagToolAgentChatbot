package com.vfu.chatbot.ai;

import com.vfu.chatbot.exception.AiToolException;
import com.vfu.chatbot.model.SessionEntity;
import com.vfu.chatbot.service.SessionService;
import com.vfu.chatbot.service.StreamXService;
import com.vfu.chatbot.service.domain.PropertyResponse;
import com.vfu.chatbot.service.domain.ReservationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static org.springframework.web.util.WebUtils.getSessionId;

@Component
@Slf4j
public class ChatBotTools {

    private final StreamXService streamXService;
    private final SessionService sessionService;

    public ChatBotTools(StreamXService streamXService, SessionService sessionService) {
        this.streamXService = streamXService;
        this.sessionService = sessionService;
    }

    @Tool(description = "Search hotel policies and rules by question")
    public String policy_rag_tool(
            @ToolParam(description = "Policy question to search") String userQuestion) {

        log.info("Policy RAG search: {}", userQuestion);
//        List<String> chunks = policyRagService.searchPolicyChunks(question);
//        return String.join("\n", chunks);
        return "Yet to implement RAG Search";
    }

    @Tool(description = """
            Get detailed property information. 
            REQUIRES propertyId (numeric ID from reservation_info_tool response).
            """)
    public PropertyResponse property_info_tool(ToolContext toolContext)
            throws AiToolException {

        String sessionId = String.valueOf(toolContext.getContext().get("sessionId"));
        Optional<SessionEntity> activeSession = sessionService.getActiveSession(sessionId);

        if (activeSession.isEmpty()) {
            throw new AiToolException("Missing Session ID");
        }
        String propertyId = activeSession.get().getUnitId();

        log.info("Property info requested for unitId: {}", propertyId);
        if (!propertyId.matches("\\d+")) {
            throw new AiToolException("Invalid propertyId. Must be numeric ID from reservation.");
        }
        PropertyResponse propertyInfo = streamXService.getPropertyInfo(propertyId);
        if (propertyInfo == null) {
            throw new AiToolException("Property not found");
        }
        return propertyInfo;
    }


    @Tool(description = """
            Verifies reservation ownership and returns details.
            REQUIRES BOTH reservationId (6-digit) + lastName + sessionId for toolContext.
            Returns JSON with propertyId needed for property_info_tool.
            """)
    public ReservationResponse reservation_info_tool(
            @ToolParam(description = "5-digit reservation ID") String reservationId,
            @ToolParam(description = "Last name EXACTLY as on booking") String lastName, ToolContext toolContext)
            throws AiToolException {

        String sessionId = String.valueOf(toolContext.getContext().get("sessionId"));

        // Input validation
        if (!reservationId.matches("\\d{6}")) {
            throw new AiToolException("Reservation ID must be 6 digits");
        }

        log.info("Verifying reservation: {} - {}", reservationId, lastName);
        ReservationResponse reservationInfo = streamXService.getReservationInfo(reservationId);

        if (reservationInfo == null) {
            throw new AiToolException("Reservation not found");
        }


        // Null-safe case-insensitive comparison
        String resLastName = reservationInfo.getLastName();
        String resId = reservationInfo.getId();
        if ((resId == null || !resId.equalsIgnoreCase(reservationId)) ||
                (lastName == null || !lastName.equalsIgnoreCase(resLastName))) {
            sessionService.clearSession(sessionId);
            throw new AiToolException("Reservation ID and last name don't match");
        }

        sessionService.saveVerifiedReservation(sessionId, reservationId, lastName, reservationInfo.getUnitId());

        log.info("Reservation verified successfully. PropertyId: {}", reservationInfo.getUnitId());
        return reservationInfo;
    }


}
