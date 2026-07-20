package com.markettwin.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * CRDDNST01M - 인구 밀집도
 */
@Entity
@Table(name = "crddnst01m")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrowdDensity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "crowd_density_id")
    private Long crowdDensityId;

    @Column(name = "market_id", nullable = false)
    private Long marketId;

    @Column(name = "zone_id", nullable = false)
    private Long zoneId;

    @Column(name = "visitor_count")
    @Builder.Default
    private Integer visitorCount = 0;

    @Column(name = "density_score", precision = 4, scale = 2)
    private BigDecimal densityScore;

    @Column(name = "status_level", length = 10)
    private String statusLevel;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;
}
