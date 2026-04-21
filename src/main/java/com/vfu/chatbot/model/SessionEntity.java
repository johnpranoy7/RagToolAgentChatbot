package com.vfu.chatbot.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionEntity {

    @Id
    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "reservation_id", length = 50, nullable = false)
    private String reservationId;

    @Column(name = "last_name", length = 100, nullable = false)
    private String lastName;

    @Column(name = "unit_id", length = 50, nullable = false)
    private String unitId;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "verified", nullable = false)
    private boolean verified;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "cached_property_summary", columnDefinition = "TEXT")
    private String cachedPropertySummary;

    @Column(name = "cached_reservation_summary", columnDefinition = "TEXT")
    private String cachedReservationSummary;

    @PrePersist
    @PreUpdate
    public void updateExpiry() {
        this.expiresAt = LocalDateTime.now().plusHours(1);
    }
}
