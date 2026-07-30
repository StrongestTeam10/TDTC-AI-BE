package com.markettwin.backend.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * 2026-07-27 추가: SIM /simulate/scenario 응답에 새로 생긴 4개 필드 반영.
 * averageDensity/maxDensity/maxDensityZoneId/maxDensityZoneName/evacuatedCount는
 * BE가 simrslt01d에 그대로 저장하는 데 쓰인다(SimulationService 참고).
 */
public record ScenarioResultDto(
        String scenarioId,
        Instant requestedAt,
        List<List<AgentStateDto>> frames,   // 스텝별 에이전트 상태 스냅샷
        Integer evacuationTimeSeconds,       // null 가능 (대피 미완료)
        RiskScoreDto finalRiskScore,
        Double averageDensity,
        Double maxDensity,
        Long maxDensityZoneId,
        String maxDensityZoneName,
        Integer evacuatedCount
) {
}