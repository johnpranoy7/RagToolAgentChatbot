package com.vfu.chatbot.ai;

import java.util.Optional;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.vfu.chatbot.StreamXOrchestrator;
import com.vfu.chatbot.exception.AiToolException;
import com.vfu.chatbot.model.SessionEntity;
import com.vfu.chatbot.service.GeoapifyPlacesApiService;
import com.vfu.chatbot.service.SessionService;
import com.vfu.chatbot.service.domain.GeoapifyResponse;

import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ChatBotTools {

    private final SessionService sessionService;
    private final GeoapifyPlacesApiService geoapifyPlacesApiService;
    private final StreamXOrchestrator streamXOrchestrator;
    private final PolicyRagService policyRagService;

    public ChatBotTools(SessionService sessionService, GeoapifyPlacesApiService geoapifyPlacesApiService, StreamXOrchestrator streamXOrchestrator, PolicyRagService policyRagService) {
        this.sessionService = sessionService;
        this.geoapifyPlacesApiService = geoapifyPlacesApiService;
        this.streamXOrchestrator = streamXOrchestrator;
        this.policyRagService = policyRagService;
    }

    @Timed(value = "chatbot.tool.policy_rag", description = "Vector policy search")
    @Tool(description = "Search hotel policies and rules by question")
    public String policy_rag_tool(
            @ToolParam(description = "Policy question to search") String userQuestion, ToolContext toolContext) {

        log.info("Policy RAG search: {}", userQuestion);
        return policyRagService.searchPolicy(userQuestion);
    }

    @Timed(value = "chatbot.tool.property_info", description = "Property lookup")
    @Tool(description = """
            Returns property details for the user's verified reservation.
            The propertyId is automatically retrieved from the verified session.
            Results are cached — safe to call multiple times for different property questions.
            """)
    public String property_info_tool(ToolContext toolContext)
            throws AiToolException {

        String sessionId = String.valueOf(toolContext.getContext().get("sessionId"));
        try {
            Optional<SessionEntity> activeSession = sessionService.getActiveSession(sessionId);

            if (activeSession.isEmpty()) {
                throw new AiToolException("Property lookup requires a verified reservation. Please verify your reservation first.");
            }
            String propertyId = activeSession.get().getUnitId();

            log.info("Property Tool search requested for sessionId:{}, propertyId:{}", activeSession.get(), propertyId);

            return streamXOrchestrator.getPropertySummary(activeSession.get(), sessionId);

        } catch (Exception ex) {
            log.error("UNEXPECTED ERROR in property_info_tool for sessionId:'{}' : {}",
                    sessionId, ex.getMessage(), ex);
            throw new AiToolException("Internal error processing reservation");
        }
    }


    @Timed(value = "chatbot.tool.reservation_info", description = "Reservation lookup")
    @Tool(description = """
            Verifies reservation ownership and returns details.
            Requires confirmationId (6-digit) and lastName exactly as on booking.
            Results are cached — returns instantly if already verified.
            """)
    public String reservation_info_tool(
            @ToolParam(description = "6-digit confirmation ID") String confirmationId,
            @ToolParam(description = "Last name EXACTLY as on booking") String lastName, ToolContext toolContext)
            throws AiToolException {

        String sessionId = String.valueOf(toolContext.getContext().get("sessionId"));

        if (!confirmationId.matches("\\d{6}")) {
            throw new AiToolException("Reservation ID must be 6 digits");
        }

        log.info("Reservation tool called for sessionId:{}, confirmationId:{}, lastName:{}",
                sessionId, confirmationId, lastName);

        return streamXOrchestrator.getReservationSummary(sessionId, confirmationId, lastName);
    }

    @Timed(value = "chatbot.tool.nearby_places", description = "Geoapify places lookup")
    @Tool(description = """
            Finds nearby attractions, restaurants, grocery stores and services
                    within 10 miles of the verified property using Geoapify Places API.
            
                    Requires property_info_tool() called first (provides lat/long).
                    Default: tourism.attraction,tourism.sights,heritage,leisure.park,leisure.picnic
                    "restaurants" → catering.restaurant,catering.fast_food,catering.cafe,catering.pub
                    "grocery"     → commercial.supermarket,commercial.convenience
                    "pharmacy"    → healthcare.pharmacy
            
                    Use for: "What's nearby?", "Restaurants?", "Grocery store?", "Things to do?"
            """)
    public GeoapifyResponse nearby_places_tool(
            @ToolParam(description = "Category hint: 'restaurants', 'grocery', 'pharmacy'. Leave empty for default attractions.")
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
