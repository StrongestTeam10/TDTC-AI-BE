package com.markettwin.backend.dto.response;

public record AgentStateDto(
        Long agentId,
        Long zoneId,
        Double x,
        Double y,
        Double latitude,
        Double longitude,
        String state    // "normal" | "congested" | "evacuating"
) {
}
