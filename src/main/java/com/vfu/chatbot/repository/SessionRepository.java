package com.vfu.chatbot.repository;

import com.vfu.chatbot.model.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<SessionEntity, String> {

    @Query("SELECT s FROM SessionEntity s WHERE s.sessionId = :sessionId AND s.verified = true AND s.expiresAt > CURRENT_TIMESTAMP")
    Optional<SessionEntity> findActiveBySessionId(@Param("sessionId") String sessionId);

    @Query("DELETE FROM SessionEntity s WHERE s.expiresAt < :now")
    int deleteAllByExpiresAtBefore(@Param("now") LocalDateTime now);

}
