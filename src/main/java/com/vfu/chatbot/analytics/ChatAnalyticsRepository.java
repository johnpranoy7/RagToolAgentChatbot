package com.vfu.chatbot.analytics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatAnalyticsRepository extends JpaRepository<ChatAnalytics, Long> {

    // Dashboard stats
    @Query(value = "SELECT COUNT(*) FROM chat_analytics WHERE created_at > NOW() - INTERVAL '24 hours'", nativeQuery = true)
    long countChatsLast24h();

    @Query(value = "SELECT AVG(confidence_score) FROM chat_analytics WHERE confidence_score IS NOT NULL", nativeQuery = true)
    Double averageConfidence();

    // Top sources
    @Query(value = "SELECT source, COUNT(*) as count FROM chat_analytics " +
            "GROUP BY source ORDER BY count DESC LIMIT 5", nativeQuery = true)
    List<Object[]> findTopSources();

    // Daily active users
    @Query(value = "SELECT ip_address, COUNT(DISTINCT session_id) as sessions, " +
            "AVG(confidence_score) as avg_confidence " +
            "FROM chat_analytics WHERE created_at > NOW() - INTERVAL '1 day' " +
            "GROUP BY ip_address ORDER BY sessions DESC", nativeQuery = true)
    List<Object[]> findDailyActiveUsers();
}

