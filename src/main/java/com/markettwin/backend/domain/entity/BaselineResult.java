package com.markettwin.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * SIMBSLN01D - 현행안 예측 결과
 *
 * 2026-07-27 신규(ERD 반영). ScenarioResult(대안 예측 결과)와 필드 구성이
 * 거의 동일하나, baseline_id로 Baseline을 참조한다는 점만 다름. ScenarioResult가
 * baseline_result_id로 이 테이블의 결과를 참조해서 대안-현행안 비교를 한다.
 */
@Entity
@Table(name = "simbsln01d")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BaselineResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "baseline_result_id")
    private Long baselineResultId;

    @Column(name = "baseline_id", nullable = false)
    private Long baselineId;

    @Column(name = "agent_count", nullable = false)
    private Integer agentCount;

    @Column(name = "predicted_max_density", precision = 6, scale = 2)
    private BigDecimal predictedMaxDensity;

    @Column(name = "predicted_density", nullable = false, precision = 6, scale = 2)
    private BigDecimal predictedDensity;

    @Column(name = "predicted_risk_score")
    private Integer predictedRiskScore;

    @Column(name = "max_density_zone_id")
    private Long maxDensityZoneId;

    @Column(name = "max_density_zone_name", length = 50)
    private String maxDensityZoneName;

    @Column(name = "evacuated_count")
    private Integer evacuatedCount;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;
}
