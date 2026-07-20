package com.markettwin.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * MRKRISK01M - 위험 척수
 */
@Entity
@Table(name = "mrkrisk01m")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Risk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "risk_id")
    private Long riskId;

    @Column(name = "market_id", nullable = false)
    private Long marketId;

    @Column(name = "zone_id", nullable = false)
    private Long zoneId;

    @Column(name = "risk_score", nullable = false)
    private Float riskScore;

    @Column(name = "risk_level", nullable = false, length = 10)
    private String riskLevel;

    @Column(name = "reason_code", nullable = false, length = 200)
    private String reasonCode;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;
}
