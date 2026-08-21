package com.markettwin.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

/**
 * scenarioType/eventZoneId/eventIntensity 삭제. 프론트에 입력창만
 * 있었을 뿐 실제로는 어디서도 읽히지 않는 죽은 필드였다(화재/음향 이벤트가
 * SIM에 구현되지 않았었음). events로 대체해 실제 효과를 내도록 했다.
 */
public record ScenarioRequestDto(
        @NotNull
        Long marketId,

        // agentCount는 '추가 유입 인원'. 0 허용 - 0이면 관측 인원만으로 시뮬.
        @NotNull @Min(0) @Max(1000)
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
        List<Long> closedGateIds,

        // 관측 프레임 선택 기준 시각. FE가 개입 전(predict)과 동일한
        // 값을 보내면 BE가 같은 프레임을 골라 개입 전/후 관측 초기배치가 일치한다.
        Instant capturedAt,

        // CCTV 관측 초기배치. BE가 채운다(FE는 안 보냄). null이면 빈 목록.
        List<ObservedAgentDto> observedAgents
) {
        public ScenarioRequestDto {
                if (observedAgents == null) {
                        observedAgents = List.of();
                }
        }

        /** BE가 관측 좌표를 계산한 뒤 그 값으로 채운 새 요청을 만든다(레코드 불변). */
        public ScenarioRequestDto withObservedAgents(List<ObservedAgentDto> agents) {
                return new ScenarioRequestDto(
                        marketId, agentCount, steps, objects, corridorPolicies,
                        events, closedGateIds, capturedAt, agents);
        }
}