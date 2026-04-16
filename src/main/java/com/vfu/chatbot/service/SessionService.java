package com.vfu.chatbot.service;


import com.vfu.chatbot.model.SessionEntity;
import com.vfu.chatbot.repository.SessionRepository;
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

        SessionEntity entity = sessionRepository.findById(sessionId)
                .orElseGet(() -> SessionEntity.builder()
                        .sessionId(sessionId)
                        .reservationId("")
                        .lastName("")
                        .unitId("")
                        .verified(false)
                        .expiresAt(LocalDateTime.now().plusMinutes(60))
                        .build());

        entity.setReservationId(reservationId);
        entity.setLastName(lastName);
        entity.setUnitId(unitId);
        entity.setVerified(true);
        entity.setExpiresAt(LocalDateTime.now().plusMinutes(60));

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
    public void cachePropertySummary(String sessionId, String propertySummary) {
        sessionRepository.findById(sessionId).ifPresent(session -> {
            session.setCachedPropertySummary(propertySummary);
            sessionRepository.save(session);
            log.info("Cached property summary for sessionId: {}", sessionId);
        });
    }

    @Transactional
    public void cacheReservationSummary(String sessionId, String reservationSummary) {
        sessionRepository.findById(sessionId).ifPresent(session -> {
            session.setCachedReservationSummary(reservationSummary);
            sessionRepository.save(session);
            log.info("Cached reservation summary for sessionId: {}", sessionId);
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
        log.info("Expired session cleanup finished: removed {} row(s)", deleted);
    }
}

