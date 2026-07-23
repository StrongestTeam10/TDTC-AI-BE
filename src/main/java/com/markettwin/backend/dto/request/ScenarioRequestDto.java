package com.markettwin.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ScenarioRequestDto(
        @NotNull
        Long marketId,

        @NotNull @Min(1) @Max(1000)
        Integer agentCount,

        @NotNull
        String scenarioType,   // "none" | "fire" | "acoustic_anomaly" | "corridor_block"

        // SIM ScenarioRequest.eventZoneId는 Optional(nullable) 이므로 @NotNull 두지 않음.
        // scenarioType이 "none"일 때는 이벤트 발생 구역이 없을 수 있다.
        Long eventZoneId,

        @NotNull @DecimalMin("0.0") @DecimalMax("1.0")
        Double eventIntensity,

        @NotNull @Min(1) @Max(1000)
        Integer steps,

        @Valid
        List<PlacedObjectDto> objects,

        @Valid
        List<CorridorPolicyDto> corridorPolicies
) {
}