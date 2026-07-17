package com.markettwin.backend.dto.response;

public record AgentStateDto(
        Long agentId,
        Long nodeId,
        Double x,
        Double y,
        String state    // "normal" | "congested" | "evacuating"
) {
}
