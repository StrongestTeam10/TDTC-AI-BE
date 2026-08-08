package com.markettwin.backend.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * 예측 시뮬레이션 응답. SIM PredictResult와 1:1 매칭.
 */
public record PredictResultDto(
        String predictionId,
        Instant requestedAt,
        List<List<AgentStateDto>> frames,
        List<RiskTrendPointDto> riskTrend,
        Double finalOverallRiskScore,
        Integer agentCount,
        Double averageDensity,
        Double maxDensity,
        Long maxDensityZoneId,
        String maxDensityZoneName,
        Integer evacuatedCount,
        // 2026-08-XX 추가: 개입 전 대피 완료 시간(개입 후와 비교용). null 가능(대피 미완료).
        Integer evacuationTimeSeconds
) {
}
