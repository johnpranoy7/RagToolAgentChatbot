package com.vfu.chatbot.analytics;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_analytics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "user_message", columnDefinition = "TEXT", nullable = false)
    private String userMessage;

    @Column(name = "bot_response", columnDefinition = "TEXT", nullable = false)
    private String botResponse;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "source")
    private String source;

}

