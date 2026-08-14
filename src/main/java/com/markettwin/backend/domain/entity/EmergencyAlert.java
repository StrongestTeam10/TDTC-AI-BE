package com.markettwin.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "emgalrt01h")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_id")
    private Long alertId;

    @Column(name = "zone_id", nullable = false)
    private Long zoneId;

    @Column(name = "alert_type", length = 50)
    private String alertType;

    @Column(name = "is_resolved")
    @Builder.Default
    private Boolean isResolved = false;

    @Column(name = "alerted_at", nullable = false)
    @Builder.Default
    private Instant alertedAt = Instant.now();

    public void resolve() {
        this.isResolved = true;
    }

}
