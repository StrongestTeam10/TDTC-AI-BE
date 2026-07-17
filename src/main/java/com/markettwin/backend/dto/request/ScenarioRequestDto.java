package com.markettwin.backend.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ScenarioRequestDto(
        @NotNull @Min(1) @Max(1000)
        Integer agentCount,

        @NotNull
        String scenarioType,   // "none" | "fire" | "acoustic_anomaly" | "corridor_block"

        @NotNull
        Long eventNodeId,

        @NotNull @DecimalMin("0.0") @DecimalMax("1.0")
        Double eventIntensity,

        @NotNull @Min(1) @Max(1000)
        Integer steps
) {
}
