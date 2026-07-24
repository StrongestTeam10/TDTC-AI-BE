package com.markettwin.backend.dto.response;

import java.util.List;

/**
 * 파이프라인 A(센서 실측 기반 관제) 응답. SIM SnapshotResponse와 1:1 매칭.
 *
 * 2026-07-24: 기존 snapshotTime/crowdDensities/risks 구조(BE가 CRDDNST01M/MRKRISK01M을
 * DB에서 직접 조회하던 방식)를 폐기하고, SIM이 실제로 시뮬레이션을 돌려 산출한 결과 구조로
 * 전면 교체함. 이제 BE는 SimulationEngineClient를 통해 SIM /simulate/snapshot을 호출하고
 * 그 응답을 그대로 이 구조로 매핑한다.
 */
public record DashboardSnapshotDto(
        Long marketId,
        String marketName,
        String mode,
        Integer step,
        Double overallRiskScore,
        List<ZoneResultDto> zones,
        List<AgentStateDto> agents,
        Integer persistedRiskRows
) {
}
