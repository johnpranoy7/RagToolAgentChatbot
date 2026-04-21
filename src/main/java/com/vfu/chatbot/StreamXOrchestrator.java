package com.vfu.chatbot;

import com.vfu.chatbot.exception.AiToolException;
import com.vfu.chatbot.model.SessionEntity;
import com.vfu.chatbot.service.SessionService;
import com.vfu.chatbot.service.StreamXFactsFormatter;
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
    private final StreamXFactsFormatter factsFormatter;

    public StreamXOrchestrator(StreamXService streamXService, SessionService sessionService,
                               StreamXFactsFormatter factsFormatter) {
        this.streamXService = streamXService;
        this.sessionService = sessionService;
        this.factsFormatter = factsFormatter;
    }

    /**
     * Returns cached property summary when present; otherwise loads from StreamX and caches summary only.
     */
    public String getPropertySummary(SessionEntity activeSession, String sessionId) throws AiToolException {
        String cached = activeSession.getCachedPropertySummary();
        if (cached != null && !cached.isBlank()) {
            log.info(">>> property_info_tool CACHE HIT (summary) for sessionId:{}", sessionId);
            return cached;
        }
        log.info("property_info_tool call to StreamX API for sessionId:{}", sessionId);
        return fetchAndCachePropertySummary(activeSession, sessionId);
    }

    /**
     * Returns cached reservation summary when present; otherwise fetches from StreamX, validates
     * confirmation id and last name against the API payload, then caches summary only.
     */
    public String getReservationSummary(String sessionId, String confirmationId, String lastName) throws AiToolException {
        Optional<SessionEntity> activeSession = sessionService.getActiveSession(sessionId);
        if (activeSession.isPresent()) {
            String cached = activeSession.get().getCachedReservationSummary();
            if (cached != null && !cached.isBlank()) {
                log.info(">>> reservation_info_tool CACHE HIT (summary) for sessionId:{}", sessionId);
                return cached;
            }
        }

        log.info(">>> reservation_info_tool calling StreamX API for sessionId:{}", sessionId);
        return fetchAndCacheReservationSummary(sessionId, confirmationId, lastName);
    }

    private String fetchAndCachePropertySummary(SessionEntity session, String sessionId) throws AiToolException {
        String propertyId = session.getUnitId();
        if (propertyId == null || !propertyId.matches("\\d+")) {
            throw new AiToolException("Invalid propertyId. Must be numeric ID from reservation.");
        }
        try {
            PropertyResponse propertyInfo = streamXService.getPropertyInfo(propertyId);
            if (propertyInfo == null) {
                throw new AiToolException("Property not found");
            }

            sessionService.updateSessionLocation(sessionId, propertyInfo.getLatitude(), propertyInfo.getLongitude());

            String summary = factsFormatter.propertyFacts(propertyInfo);
            sessionService.cachePropertySummary(sessionId, summary);
            return summary;
        } catch (AiToolException ex) {
            throw ex;
        } catch (Exception e) {
            log.error("StreamX API FAILED for propertyId='{}': {}", propertyId, e.getMessage(), e);
            throw new AiToolException("Error fetching property data: " + e.getMessage());
        }
    }

    private String fetchAndCacheReservationSummary(String sessionId, String confirmationId, String lastName)
            throws AiToolException {
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

        verifyReservationMatches(reservationInfo, confirmationId, lastName);

        sessionService.saveVerifiedReservation(
                sessionId, confirmationId, lastName, reservationInfo.getUnitId());

        String summary = factsFormatter.reservationFacts(reservationInfo);
        sessionService.cacheReservationSummary(sessionId, summary);

        log.info("Reservation verified and cached (summary only). unitId:{}", reservationInfo.getUnitId());
        return summary;
    }

    private static void verifyReservationMatches(ReservationResponse reservationInfo,
                                                 String confirmationId,
                                                 String lastName) throws AiToolException {
        String resLastName = reservationInfo.getLastName();
        String resId = reservationInfo.getConfirmationId();
        if ((resId == null || !resId.equalsIgnoreCase(confirmationId))
                || (lastName == null || !lastName.equalsIgnoreCase(resLastName))) {
            throw new AiToolException("Reservation ID and last name don't match");
        }
    }
}
