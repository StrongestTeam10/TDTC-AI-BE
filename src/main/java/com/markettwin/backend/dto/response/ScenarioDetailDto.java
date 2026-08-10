package com.markettwin.backend.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * 2026-08-08 추가
 * 시나리오 한 건의 실행 설정. 목록에서 행을 펼쳤을 때 보여준다.
 *
 * 목록의 시나리오명은 "{날짜} {시장} {정책유형} 시나리오" 형태라, 같은 시장에서 같은
 * 유형을 여러 번 돌리면 서로 구분되지 않는다. 어느 구역에 화재를 냈고 무엇을 배치했는지는
 * simscnr01m.virtual_config(실행 요청 전체가 담긴 JSON)에만 남아 있어 화면에서 볼 수
 * 없었다. 그 JSON을 풀어 사람이 읽을 수 있는 형태로 돌려준다.
 *
 * 구역 이름(zoneName)은 virtual_config에 없다. 저장된 것은 zoneId뿐이므로 조회 시점에
 * mrkzone01m에서 찾아 채운다. 지워진 구역을 가리키면 null이 되고 화면은 번호만 보여준다.
 */
public record ScenarioDetailDto(
        Long scenarioId,

        // 목록과 같은 규칙으로 만든 표시용 이름(ScenarioDisplayNameResolver).
        String scenarioName,

        Long marketId,
        String marketName,

        // 실행 조건.
        Integer agentCount,
        Integer steps,
        String policyTypeCode,

        // 시나리오 등록(실행 요청) 시각.
        Instant regDatetime,

        List<PlacedObjectView> objects,
        List<EventTriggerView> events,
        List<CorridorPolicyView> corridorPolicies,
        List<GateView> closedGates
) {

    /** 배치한 오브젝트 하나. */
    public record PlacedObjectView(
            String objectType,
            Long zoneId,
            String zoneName,
            Double intensity
    ) {
    }

    /** 발생시킨 이벤트 하나. */
    public record EventTriggerView(
            String eventType,
            Long zoneId,
            String zoneName,
            Double intensity,
            Integer triggerStep,
            Integer burnSteps,
            Integer recoverySteps
    ) {
    }

    /** 통로 정책 하나. */
    public record CorridorPolicyView(
            Long fromZoneId,
            String fromZoneName,
            Long toZoneId,
            String toZoneName,
            String action,
            String allowedDirection
    ) {
    }

    /** 닫은 게이트 하나. 이름은 시설(facility)에서 찾는다. */
    public record GateView(
            Long facilityId,
            String name
    ) {
    }
}
