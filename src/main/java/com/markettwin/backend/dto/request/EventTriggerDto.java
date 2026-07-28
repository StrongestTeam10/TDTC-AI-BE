package com.markettwin.backend.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * 2026-07-25 추가: 화재/음향 이상 이벤트. SIM의 EventTrigger와 1:1 매칭.
 * 오브젝트 배치와 같은 방식(지도 클릭 -> zoneId + 위경도 + intensity)으로 배치한다.
 */
public record EventTriggerDto(
        @NotNull
        String eventType,   // "fire" | "acoustic_anomaly"

        @NotNull
        Long zoneId,

        @DecimalMin("0.0") @DecimalMax("1.0")
        Double intensity,

        Double latitude,
        Double longitude
) {
}