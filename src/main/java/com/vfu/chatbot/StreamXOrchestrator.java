package com.vfu.chatbot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vfu.chatbot.exception.AiToolException;
import com.vfu.chatbot.model.SessionEntity;
import com.vfu.chatbot.service.SessionService;
import com.vfu.chatbot.service.StreamXService;
import com.vfu.chatbot.service.domain.PropertyResponse;
import com.vfu.chatbot.service.domain.ReservationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class StreamXOrchestrator {

    private final StreamXService streamXService;
    private final SessionService sessionService;
    private final ObjectMapper objectMapper;

    public StreamXOrchestrator(StreamXService streamXService, SessionService sessionService, ObjectMapper objectMapper) {
        this.streamXService = streamXService;
        this.sessionService = sessionService;
        this.objectMapper = objectMapper;
    }

    public PropertyResponse getPropertyResponse(SessionEntity activeSession, String sessionId) throws AiToolException {
        PropertyResponse propertyInfo;
        if (activeSession.getCachedPropertyResponse() != null) {
            log.info(">>> property_info_tool CACHE HIT for sessionId:{}", sessionId);
            try {
                propertyInfo = objectMapper.readValue(
                        activeSession.getCachedPropertyResponse(), PropertyResponse.class);
            } catch (Exception e) {
                log.warn("Cache deserialize failed, falling back to API: {}", e.getMessage());
                propertyInfo = fetchAndCacheProperty(activeSession, sessionId);
            }
        } else {
            log.info("property_info_tool call to StreamX API for sessionId:{}", sessionId);
            propertyInfo = fetchAndCacheProperty(activeSession, sessionId);
        }
        return propertyInfo;
    }

    private PropertyResponse fetchAndCacheProperty(SessionEntity session,
                                                         String sessionId) throws AiToolException {
        String propertyId = session.getUnitId();
        if (!propertyId.matches("\\d+")) {
            throw new AiToolException("Invalid propertyId. Must be numeric ID from reservation.");
        }
        try {
            PropertyResponse propertyInfo = streamXService.getPropertyInfo(propertyId);
            if (propertyInfo == null) throw new AiToolException("Property not found");

            // Update lat/long in session
            sessionService.updateSessionLocation(
                    sessionId, propertyInfo.getLatitude(), propertyInfo.getLongitude());

            // Cache the full response
            sessionService.cachePropertyResponse(
                    sessionId, objectMapper.writeValueAsString(propertyInfo));

            return propertyInfo;
        } catch (AiToolException ex) {
            throw ex;
        } catch (Exception e) {
            log.error("StreamX API FAILED for propertyId='{}': {}", propertyId, e.getMessage(), e);
            throw new AiToolException("Error fetching property data: " + e.getMessage());
        }
    }


    public ReservationResponse getReservationResponse(String sessionId,
                                                      String confirmationId,
                                                      String lastName) throws AiToolException {

        Optional<SessionEntity> activeSession = sessionService.getActiveSession(sessionId);
        if (activeSession.isPresent()
                && activeSession.get().getCachedReservationResponse() != null) {
            log.info(">>> reservation_info_tool CACHE HIT for sessionId:{}", sessionId);
            try {
                return objectMapper.readValue(
                        activeSession.get().getCachedReservationResponse(),
                        ReservationResponse.class);
            } catch (Exception e) {
                log.warn("Reservation cache deserialize failed, falling back to API: {}", e.getMessage());
            }
        }

        log.info(">>> reservation_info_tool calling StreamX API for sessionId:{}", sessionId);
        return fetchAndCacheReservation(sessionId, confirmationId, lastName);
    }

    private ReservationResponse fetchAndCacheReservation(String sessionId,
                                                         String confirmationId,
                                                         String lastName) throws AiToolException {
        ReservationResponse reservationInfo;
        try {
            reservationInfo = streamXService.getReservationInfo(confirmationId);
        } catch (Exception e) {
            log.error("StreamX API FAILED for confirmationId='{}', sessionId='{}': {}",
                    confirmationId, sessionId, e.getMessage(), e);
            throw new AiToolException("Error fetching reservation data: " + e.getMessage());
        }

        if (reservationInfo == null) {
            log.error("Null reservation response for confirmationId='{}', sessionId='{}'",
                    confirmationId, sessionId);
            throw new AiToolException("Reservation not found");
        }

        // Verify last name matches
        String resLastName = reservationInfo.getLastName();
        String resId = reservationInfo.getConfirmationId();
        if ((resId == null || !resId.equalsIgnoreCase(confirmationId)) ||
                (lastName == null || !lastName.equalsIgnoreCase(resLastName))) {
            throw new AiToolException("Reservation ID and last name don't match");
        }

        // Save verified session
        sessionService.saveVerifiedReservation(
                sessionId, confirmationId, lastName, reservationInfo.getUnitId());

        // Cache the response
        try {
            sessionService.cacheReservationResponse(
                    sessionId, objectMapper.writeValueAsString(reservationInfo));
        } catch (Exception e) {
            log.warn("Failed to cache reservation response: {}", e.getMessage());
        }

        log.info("Reservation verified and cached. unitId:{}", reservationInfo.getUnitId());
        return reservationInfo;
    }


}
