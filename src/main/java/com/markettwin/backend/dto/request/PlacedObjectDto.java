package com.markettwin.backend.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/** SIM의 PlacedObject와 1:1 매칭. 지도 위에 배치한 오브젝트 하나. */
public record PlacedObjectDto(
        @NotNull
        String objectType,   // "food_truck" | "obstacle" | "event_zone" | "rest_area"

        @NotNull
        Long zoneId,

        @DecimalMin("0.0") @DecimalMax("1.0")
        Double intensity
) {
}