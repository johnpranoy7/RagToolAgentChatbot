package com.vfu.chatbot.analytics;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatAnalyticsService {

    private final ChatAnalyticsRepository repository;

    public ChatAnalytics logChat(String sessionId, String userMessage, String botResponse,
                                 Double confidenceScore, String source) {
        ChatAnalytics analytics = ChatAnalytics.builder()
                .sessionId(sessionId)
                .userMessage(userMessage)
                .botResponse(botResponse)
                .confidenceScore(confidenceScore)
                .source(source)
                .createdAt(LocalDateTime.now())
                .build();

        return repository.save(analytics);
    }

    // Dashboard data
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalChats24h", repository.countChatsLast24h());
        stats.put("avgConfidence", repository.averageConfidence());
        stats.put("topSources", repository.findTopSources());
        return stats;
    }

    public List<Object[]> getDailyActiveUsers() {
        return repository.findDailyActiveUsers();
    }
}

