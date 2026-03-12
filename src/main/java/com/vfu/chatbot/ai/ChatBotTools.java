package com.vfu.chatbot.ai;

import com.vfu.chatbot.exception.AiToolException;
import com.vfu.chatbot.model.SessionEntity;
import com.vfu.chatbot.service.SessionService;
import com.vfu.chatbot.service.StreamXService;
import com.vfu.chatbot.service.domain.PropertyResponse;
import com.vfu.chatbot.service.domain.ReservationResponse;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.boot.jaxb.hbm.transform.PropertyInfo;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ChatBotTools {

    private final StreamXService streamXService;
    private final SessionService sessionService;
    private final PgVectorStore vectorStore;

    public ChatBotTools(StreamXService streamXService, SessionService sessionService, PgVectorStore vectorStore) {
        this.streamXService = streamXService;
        this.sessionService = sessionService;
        this.vectorStore = vectorStore;
    }

    @Tool(description = "Search hotel policies and rules by question")
    public String policy_rag_tool(
            @ToolParam(description = "Policy question to search") String userQuestion, ToolContext toolContext) {

        log.info("Policy RAG search: {}", userQuestion);
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder().query(userQuestion).topK(6).build()
        );

        if (results.isEmpty()) {
            return "Contact Customer Service: 1-800-555-1234";
        }

        return results.stream()
                .map(Document::getFormattedContent)
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    @Tool(description = """
            Returns property details for the user's verified reservation.
            
            This tool does NOT accept propertyId from the user.
            The propertyId is automatically retrieved from the verified session.
            
            Use ONLY after reservation_info_tool succeeds.
            """)
    public PropertyResponse property_info_tool(ToolContext toolContext)
            throws AiToolException {

        String sessionId = String.valueOf(toolContext.getContext().get("sessionId"));
        try {
            Optional<SessionEntity> activeSession = sessionService.getActiveSession(sessionId);

            if (activeSession.isEmpty()) {
                throw new AiToolException("Missing Session ID");
            }
            String propertyId = activeSession.get().getUnitId();

            log.info("Property Tool search requested for sessionId:{}, propertyId:{}", activeSession.get(), propertyId);

            if (!propertyId.matches("\\d+")) {
                throw new AiToolException("Invalid propertyId. Must be numeric ID from reservation.");
            }
            PropertyResponse propertyInfo;
            try {
                propertyInfo = streamXService.getPropertyInfo(propertyId);
            } catch (Exception e) {
                log.error("STREAMX API FAILED for propertyId='{}', sessionId='{}': {}",
                        propertyId, sessionId, e.getMessage(), e);
                throw new AiToolException("Error Fetching Property Data: " + e.getMessage());
            }

            if (propertyInfo == null) {
                throw new AiToolException("Property not found");
            }
            return propertyInfo;
        } catch (Exception ex) {
            log.error("UNEXPECTED ERROR in property_info_tool for sessionId:'{}' : {}",
                    sessionId, ex.getMessage(), ex);
            throw new AiToolException("Internal error processing reservation");
        }
    }


    @Tool(description = """
            Verifies reservation ownership and returns details.
            REQUIRES BOTH confirmationId (6-digit) + lastName + sessionId for toolContext.
            Returns JSON with propertyId needed for property_info_tool.
            """)
    public ReservationResponse reservation_info_tool(
            @ToolParam(description = "6-digit confirmation ID") String confirmationId,
            @ToolParam(description = "Last name EXACTLY as on booking") String lastName, ToolContext toolContext)
            throws AiToolException {

        String sessionId = String.valueOf(toolContext.getContext().get("sessionId"));
        try {
            // Input validation
            if (!confirmationId.matches("\\d{6}")) {
                throw new AiToolException("Reservation ID must be 6 digits");
            }

            log.info("Verifying reservation: {} - {}", confirmationId, lastName);
            log.info("Reservation Tool search requested for sessionId:{}, confirmationId:{}, lastName:{}",
                    sessionId, confirmationId, lastName);

            ReservationResponse reservationInfo;
            try {
                reservationInfo = streamXService.getReservationInfo(confirmationId);
            } catch (Exception e) {
                log.error("STREAMX API FAILED for confirmationId='{}', sessionId='{}': {}",
                        confirmationId, sessionId, e.getMessage(), e);
                throw new AiToolException("Error Fetching Reservation Data: " + e.getMessage());
            }

            if (reservationInfo == null) {
                log.error("RESERVATION NULL RESPONSE from streamXService for confirmationId='{}', sessionId='{}'", confirmationId, sessionId);
                throw new AiToolException("Reservation not found");
            }

            // Null-safe case-insensitive comparison
            String resLastName = reservationInfo.getLastName();
            String resId = reservationInfo.getConfirmationId();
            if ((resId == null || !resId.equalsIgnoreCase(confirmationId)) ||
                    (lastName == null || !lastName.equalsIgnoreCase(resLastName))) {
                sessionService.clearSession(sessionId);
                throw new AiToolException("Reservation ID and last name don't match");
            }

            sessionService.saveVerifiedReservation(sessionId, confirmationId, lastName, reservationInfo.getUnitId());

            log.info("Reservation verified successfully. PropertyId: {}", reservationInfo.getUnitId());
            return reservationInfo;
        } catch (Exception ex) {
            log.error("UNEXPECTED ERROR in reservation_info_tool - sessionId:'{}', confirmationId:'{}', lastName:'{}': {}",
                    sessionId, confirmationId, lastName, ex.getMessage(), ex);
            throw new AiToolException("Internal error processing reservation");
        }
    }


}
