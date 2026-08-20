package com.markettwin.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

/**
 * SIMRSLT01D - 시나리오 예측 결과
 *
 * maxDensityZoneId/maxDensityZoneName/evacuatedCount 3개 필드.
 * 보고서에서 "중앙통로에서 최대 X명/㎡" 같은 문장이나 "위험 인원이 N명 발생했다"는
 * 서술을 하려면 밀집도 숫자만으론 부족해서, 어느 구역인지 + 실제 대피 인원수를
 * 같이 저장한다. DB 컬럼도 이 3개를 추가하는 마이그레이션을 거쳤음
 * (simrslt01d.max_density_zone_id/max_density_zone_name/evacuated_count).
 *
 * flowDirection에 @JdbcTypeCode(SqlTypes.JSON) 추가.
 * columnDefinition="jsonb"만으로는 Hibernate가 실제 저장 시 문자열(character
 * varying)로 취급해서 "column is of type jsonb but expression is of type
 * character varying" 에러가 났음 - 이 어노테이션이 있어야 Hibernate가 값을
 * 실제로 jsonb로 변환해서 보낸다.
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

    // 추가(ERD 반영): 같은 시장의 현행안 결과(BaselineResult)와 짝지어서
    // "대안이 현행안 대비 얼마나 나아졌는지" 비교 보고서를 만들기 위한 참조
    @Column(name = "baseline_result_id")
    private Long baselineResultId;

    @Column(name = "predicted_max_density", precision = 6, scale = 2)
    private BigDecimal predictedMaxDensity;

    @Column(name = "predicted_density", nullable = false, precision = 6, scale = 2)
    private BigDecimal predictedDensity;

    /** 예측 위험 점수. ERD상 INT 타입 */
    @Column(name = "predicted_risk_score")
    private Integer predictedRiskScore;

    @Column(name = "economic_effect_analysis", columnDefinition = "TEXT")
    private String economicEffectAnalysis;

    @Column(name = "generated_report_path", length = 1000)
    private String generatedReportPath;

    // 추가(ERD 반영)
    @Column(name = "report_title", length = 200)
    private String reportTitle;

    // PostgreSQL INTERVAL 타입 매핑 (Hibernate 6.2+)
    @JdbcTypeCode(SqlTypes.INTERVAL_SECOND)
    @Column(name = "avg_stay_time")
    private Duration avgStayTime;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "flow_direction", columnDefinition = "jsonb")
    private String flowDirection;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    @Column(name = "max_density_zone_id")
    private Long maxDensityZoneId;

    @Column(name = "max_density_zone_name", length = 50)
    private String maxDensityZoneName;

    @Column(name = "evacuated_count")
    private Integer evacuatedCount;
}