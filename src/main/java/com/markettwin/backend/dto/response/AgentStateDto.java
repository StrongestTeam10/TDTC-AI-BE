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
        @JsonProperty("actionState") String actionState // "ENTERING" | "MOVING" | "STAYING" | "EXITING"
) {
}
