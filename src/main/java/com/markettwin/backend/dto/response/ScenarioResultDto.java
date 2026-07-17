package com.markettwin.backend.dto.response;

import java.time.Instant;
import java.util.List;

public record ScenarioResultDto(
        String scenarioId,
        Instant requestedAt,
        List<List<AgentStateDto>> frames,   // 스텝별 에이전트 상태 스냅샷
        Integer evacuationTimeSeconds,       // null 가능 (대피 미완료)
        RiskScoreDto finalRiskScore
) {
}
