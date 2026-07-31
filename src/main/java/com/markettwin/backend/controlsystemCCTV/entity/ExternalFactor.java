package com.markettwin.backend.controlsystemCCTV.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "extfctr01h")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalFactor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "factor_id")
    private Long factorId;

    @Column(name = "market_id", nullable = false)
    private Long marketId;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(name = "weather_condition", length = 50)
    private String weatherCondition;

    @Column(name = "temperature", precision = 4, scale = 1)
    private BigDecimal temperature;

    @Column(name = "event_category", length = 50)
    private String eventCategory;

    @Column(name = "event_name", length = 200)
    private String eventName;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @Column(name = "video_id")
    private Integer videoId;
}
