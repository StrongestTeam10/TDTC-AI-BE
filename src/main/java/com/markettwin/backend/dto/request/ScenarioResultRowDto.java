package com.markettwin.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.time.Instant;

/** SIM ScenarioResultRow와 1:1. simrslt01d 조회 결과 한 행. */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ScenarioResultRowDto(
        Long resultId,
        Long scenarioId,
        BigDecimal predictedMaxDensity,
        BigDecimal predictedDensity,
        Integer predictedRiskScore,
        String economicEffectAnalysis,
        String generatedReportPath,

        // ⚠️ 반드시 String이어야 한다.
        // 엔티티는 java.time.Duration인데, Jackson 설정에 따라 숫자(초)로 직렬화될 수 있다.
        // SIM의 _duration_to_minutes()는 숫자를 "분"으로 해석하므로, 30분 체류가
        // 1800분으로 둔갑한 보고서가 아무 에러 없이 나온다.
        // Duration.toString()으로 ISO-8601("PT30M")을 못박아 이 경로를 차단한다.
        String avgStayTime,

        String flowDirection,
        Instant executedAt,

        // 시뮬레이션 저장 시점부터 simrslt01d/simbsln01d에 쌓여
        // 있었지만 보고서로는 전달되지 않던 값들이다. 화재·음향 이벤트 시나리오에서
        // 정책 효과가 실제로 드러나는 지표라 보고서 본문의 근거로 쓴다.

        // 시뮬레이션 중 대피 상태로 전환된 방문객 수.
        Integer evacuatedCount,

        // 최대 밀집도가 발생한 구역. 대피 병목 지점을 문장으로 지목할 때 쓴다.
        Long maxDensityZoneId,
        String maxDensityZoneName
) {
}