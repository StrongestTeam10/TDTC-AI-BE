package com.markettwin.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * CRDDNST01H - 인구 밀집도 로그 (CRDDNST01M의 변경 이력)
 */
@Entity
@Table(name = "CRDDNST01H")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrowdDensityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "crowd_density_sq")
    private Long crowdDensitySq;

    @Column(name = "crowd_density_id", nullable = false)
    private Long crowdDensityId;

    @Column(name = "visitor_count")
    @Builder.Default
    private Integer visitorCount = 0;

    @Column(name = "density_score", precision = 4, scale = 2)
    private BigDecimal densityScore;

    @Column(name = "status_level", nullable = false, length = 10)
    private String statusLevel;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;
}
