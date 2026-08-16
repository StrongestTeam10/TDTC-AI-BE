package com.markettwin.backend.dto.response;

/**
 * 2026-08-14 추가: 구역에서 먼 건물 정리 결과.
 *
 * @param totalBuildings   이 시장에 저장돼 있던 건물 수
 * @param outsideBuildings 여유 거리를 벗어난 건물 수(dryRun이면 "지울 대상")
 * @param keptBuildings    남는(남은) 건물 수
 * @param bufferMeters     판정에 쓴 여유 거리
 * @param dryRun           true면 세기만 하고 지우지 않았다는 뜻
 */
public record BuildingPruneResultDto(
        int totalBuildings,
        int outsideBuildings,
        int keptBuildings,
        int bufferMeters,
        boolean dryRun
) {
}
