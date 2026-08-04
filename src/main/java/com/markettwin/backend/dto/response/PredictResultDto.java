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
        Integer evacuatedCount
) {
}
