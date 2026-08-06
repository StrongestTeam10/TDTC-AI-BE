package com.markettwin.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AgentStateDto(
        @JsonProperty("agentId") Long agentId,
        @JsonProperty("zoneId") Long zoneId,
        @JsonProperty("x") Double x,
        @JsonProperty("y") Double y,
        @JsonProperty("latitude") Double latitude,
        @JsonProperty("longitude") Double longitude,
        @JsonProperty("state") String state,    // "normal" | "congested" | "evacuating" (situationState in SIM)
        @JsonProperty("agentType") String agentType, // "PASS_THROUGH" | "SHOPPING" | "FOOD_TOUR"
        @JsonProperty("actionState") String actionState, // "ENTERING" | "MOVING" | "STAYING" | "EXITING"
        // 2026-08-XX 추가: 현재 구역 위험도(0~100). FE에서 위험도별 색(빨강/노랑/파랑) 표시에 사용.
        @JsonProperty("dangerLevel") Double dangerLevel
) {
}
