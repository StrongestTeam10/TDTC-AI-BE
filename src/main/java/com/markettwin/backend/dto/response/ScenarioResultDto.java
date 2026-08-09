package com.markettwin.backend.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * 2026-07-27 추가: SIM /simulate/scenario 응답에 새로 생긴 4개 필드 반영.
 * averageDensity/maxDensity/maxDensityZoneId/maxDensityZoneName/evacuatedCount는
 * BE가 simrslt01d에 그대로 저장하는 데 쓰인다(SimulationService 참고).
 *
 * 2026-08-06 추가: persistedScenarioId.
 */
public record ScenarioResultDto(
        // SIM이 실행마다 새로 만드는 UUID다. DB의 시나리오 식별자가 아니라서
        // 보고서 생성(POST /api/simulation/reports)에는 쓸 수 없다.
        // 그 용도로는 아래 persistedScenarioId를 쓸 것.
        String scenarioId,
        Instant requestedAt,
        List<List<AgentStateDto>> frames,   // 스텝별 에이전트 상태 스냅샷
        // 2026-08-XX 추가: 개입 후 스텝별 위험도 추이(개입 전과 겹쳐 비교용). SIM 응답 전달.
        List<RiskTrendPointDto> riskTrend,
        Integer evacuationTimeSeconds,       // null 가능 (대피 미완료)
        RiskScoreDto finalRiskScore,
        Double averageDensity,
        Double maxDensity,
        Long maxDensityZoneId,
        String maxDensityZoneName,
        Integer evacuatedCount,

        /*
         * 이 실행이 저장된 simscnr01m.scenario_id.
         *
         * BE는 실행 요청을 DB에 먼저 저장해 이 값을 이미 갖고 있는데
         * (SimulationService.runScenario) 지금까지 응답에 담지 않아, FE가 방금 실행한
         * 시나리오로 보고서를 만들 수단이 없었다. 위 scenarioId는 SIM이 만든 UUID여서
         * 대체할 수 없다.
         *
         * SIM 응답을 역직렬화하는 시점에는 SIM이 모르는 필드라 null이고,
         * SimulationService가 저장 직후 withPersistedScenarioId로 채워 내려준다.
         */
        Long persistedScenarioId
) {

    /** SIM 응답에 DB 시나리오 식별자만 덧붙인 사본을 만든다. */
    public ScenarioResultDto withPersistedScenarioId(Long persistedScenarioId) {
        return new ScenarioResultDto(
                scenarioId, requestedAt, frames, evacuationTimeSeconds, finalRiskScore,
                averageDensity, maxDensity, maxDensityZoneId, maxDensityZoneName,
                evacuatedCount, persistedScenarioId);
    }
}
