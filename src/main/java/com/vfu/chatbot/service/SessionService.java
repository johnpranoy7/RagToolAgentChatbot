package com.vfu.chatbot.service;


import com.vfu.chatbot.model.SessionEntity;
import com.vfu.chatbot.repository.SessionRepository;
import com.vfu.chatbot.service.domain.ReservationResponse;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private final SessionRepository sessionRepository;

    @Transactional
    public String getOrCreateSessionId(String providedSessionId) {
        if (providedSessionId != null && !providedSessionId.isEmpty()) {
            return providedSessionId; // Use frontend-provided
        }

        // Generate new UUID sessionId
        String newSessionId = UUID.randomUUID().toString();
        log.info("Created new sessionId: {}", newSessionId);
        return newSessionId;
    }

    @Timed(value = "chatbot.session.create", description = "Create new session and save")
    public void saveVerifiedReservation(
            String sessionId,
            String reservationId,
            String lastName,
            String unitId) {

        SessionEntity entity = SessionEntity.builder()
                .sessionId(sessionId)
                .reservationId(reservationId)
                .lastName(lastName)
                .unitId(unitId)
                .verified(true)
                .expiresAt(LocalDateTime.now().plusMinutes(60))
                .build();

        sessionRepository.save(entity);
        log.info("Cached session: {} → unitId: {}", sessionId, unitId);
    }

    @Transactional
    public void updateSessionLocation(String sessionId, Double latitude, Double longitude) {
        Optional<SessionEntity> optionalEntity = sessionRepository.findById(sessionId);

        if (optionalEntity.isEmpty()) {
            log.warn("Session not found for patch: {}", sessionId);
            return;
        }

        SessionEntity session = optionalEntity.get();

        session.setLatitude(latitude);
        session.setLongitude(longitude);
        session.setUpdatedAt(LocalDateTime.now());

        sessionRepository.save(session);
        log.info("Patched session {} → lat: {}, lon: {}",
                sessionId, latitude, longitude);
    }


    @Timed(value = "chatbot.session.get_active", description = "Get active session")
    public Optional<SessionEntity> getActiveSession(String sessionId) {
        return sessionRepository.findActiveBySessionId(sessionId);
    }

    @Transactional
    public void cachePropertyResponse(String sessionId, String propertyJson) {
        sessionRepository.findById(sessionId).ifPresent(session -> {
            session.setCachedPropertyResponse(propertyJson);
            sessionRepository.save(session);
            log.info("Cached property response for sessionId: {}", sessionId);
        });
    }

    @Transactional
    public void cacheReservationResponse(String sessionId, String reservationJson) {
        sessionRepository.findById(sessionId).ifPresent(session -> {
            session.setCachedReservationResponse(reservationJson);
            sessionRepository.save(session);
            log.info("Cached reservation response for sessionId: {}", sessionId);
        });
    }

    /**
     * Clear specific session
     */
    @Transactional
    public void clearSession(String sessionId) {
        sessionRepository.deleteById(sessionId);
        log.info("Cleared session: {}", sessionId);
    }

    /**
     * Clear ALL expired sessions (call periodically)
     */
    @Transactional
    public void cleanupExpiredSessions() {
        int deleted = sessionRepository.deleteAllByExpiresAtBefore(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Cleaned {} expired sessions", deleted);
        }
    }
}

