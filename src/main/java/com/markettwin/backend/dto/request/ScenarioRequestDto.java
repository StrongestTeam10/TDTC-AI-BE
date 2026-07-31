package com.markettwin.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 2026-07-25: scenarioType/eventZoneId/eventIntensity 삭제. 프론트에 입력창만
 * 있었을 뿐 실제로는 어디서도 읽히지 않는 죽은 필드였다(화재/음향 이벤트가
 * SIM에 구현되지 않았었음). events로 대체해 실제 효과를 내도록 했다.
 */
public record ScenarioRequestDto(
        @NotNull
        Long marketId,

        @NotNull @Min(1) @Max(1000)
        Integer agentCount,

        @NotNull @Min(1) @Max(1000)
        Integer steps,

        @Valid
        List<PlacedObjectDto> objects,

        @Valid
        List<CorridorPolicyDto> corridorPolicies,

        @Valid
        List<EventTriggerDto> events,

        // 지도에서 닫은 게이트(출입구)의 facility_id 목록.
        // null 허용 - 요청에 없으면 SIM 쪽 default_factory(빈 리스트)로 처리됨.
        List<Long> closedGateIds
) {
}