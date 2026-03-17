package com.vfu.chatbot.ai;

import com.vfu.chatbot.exception.AiToolException;
import com.vfu.chatbot.model.SessionEntity;
import com.vfu.chatbot.service.GeoapifyPlacesApiService;
import com.vfu.chatbot.service.SessionService;
import com.vfu.chatbot.service.StreamXService;
import com.vfu.chatbot.service.domain.GeoapifyResponse;
import com.vfu.chatbot.service.domain.PropertyResponse;
import com.vfu.chatbot.service.domain.ReservationResponse;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
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
    private final GeoapifyPlacesApiService geoapifyPlacesApiService;
    private final PgVectorStore vectorStore;

    public ChatBotTools(StreamXService streamXService, SessionService sessionService, GeoapifyPlacesApiService geoapifyPlacesApiService, PgVectorStore vectorStore) {
        this.streamXService = streamXService;
        this.sessionService = sessionService;
        this.geoapifyPlacesApiService = geoapifyPlacesApiService;
        this.vectorStore = vectorStore;
    }

    @Timed(value = "chatbot.tool.policy_rag", description = "Vector policy search")
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

    @Timed(value = "chatbot.tool.property_info", description = "Property lookup")
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
                sessionService.updateSessionLocation(activeSession.get().getSessionId(), propertyInfo.getLatitude(), propertyInfo.getLongitude());
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

    @Timed(value = "chatbot.tool.reservation_info", description = "Reservation lookup")
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

    @Timed(value = "chatbot.tool.nearby_places", description = "Geoapify places lookup")
    @Tool(description = """
            Finds nearby attractions, restaurants, grocery stores, and services within 10 miles
            of the verified property using Geoapify Places API (OpenStreetMap data).
            
            REQUIRES property_info_tool() called first (provides lat/long).
            Default: tourism.attraction,tourism.sights,heritage,leisure
            Dynamic: "restaurants" → catering.*, "grocery" → commercial.supermarket
            
            Use for: "What's nearby?", "Restaurants?", "Grocery store?", "Things to do?"
            """)
    public GeoapifyResponse nearby_places_tool(
            @ToolParam(description = "Optional: 'restaurants', 'grocery', 'shopping'. Default: attractions/parks")
            String categoryHint,
            ToolContext toolContext) throws AiToolException {

        String sessionId = String.valueOf(toolContext.getContext().get("sessionId"));
        try {
            Optional<SessionEntity> activeSession = sessionService.getActiveSession(sessionId);

            if (activeSession.isEmpty()) {
                throw new AiToolException("Missing Session ID");
            }

            SessionEntity session = activeSession.get();

            if (session.getLatitude() == null || session.getLongitude() == null) {
                throw new AiToolException("Missing Latitude and Longitude for Property");
            }

            log.info("Nearby places search requested for sessionId:{}, property:{}, ({},{})",
                    sessionId, session.getUnitId(), session.getLatitude(), session.getLongitude());

            //TODO: Modify the categories after testing
            String categories = resolveCategories(categoryHint);

            // Geoapify API call (10 miles)
            GeoapifyResponse nearbyPlaces = geoapifyPlacesApiService.findNearbyPlaces(
                    categories,
                    session.getLongitude(),
                    session.getLatitude(),
                    10000
            );

            log.info(nearbyPlaces.toString());
            return nearbyPlaces;

        } catch (Exception ex) {
            log.error("UNEXPECTED ERROR in nearby_places_tool for sessionId:'{}': {}", sessionId, ex.getMessage(), ex);
            throw new AiToolException("Internal error searching nearby places");
        }
    }

    private String resolveCategories(String categoryHint) {
        if (categoryHint == null) categoryHint = "";

        return switch (categoryHint.toLowerCase()) {
            case "restaurants", "restaurant", "food", "dining", "eat" ->
                    "catering.restaurant,catering.fast_food,catering.cafe,catering.pub";
            case "grocery", "supermarket", "groceries", "shopping" -> "commercial.supermarket,commercial.convenience";
            case "pharmacy", "drugs", "medicine" -> "healthcare.pharmacy";
            default -> "tourism.attraction,tourism.sights,heritage,leisure.park,leisure.picnic";  // Your spec defaults
        };
    }


}
