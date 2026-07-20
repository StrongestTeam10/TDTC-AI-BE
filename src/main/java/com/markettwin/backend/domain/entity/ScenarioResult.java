package com.markettwin.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * SIMRSLT01D - 시나리오 예측 결과
 */
@Entity
@Table(name = "simrslt01d")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScenarioResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "result_id")
    private Long resultId;

    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;

    @Column(name = "predicted_max_density", precision = 6, scale = 2)
    private BigDecimal predictedMaxDensity;

    @Column(name = "predicted_risk_score", precision = 6, scale = 2)
    private BigDecimal predictedRiskScore;

    @Lob
    @Column(name = "economic_effect_analysis")
    private String economicEffectAnalysis;

    @Column(name = "generated_report_path", length = 300)
    private String generatedReportPath;

    @Column(name = "avg_stay_time")
    private Integer avgStayTime;

    @Column(name = "flow_direction", columnDefinition = "jsonb")
    private String flowDirection;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;
}
