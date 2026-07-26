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
        Double intensity,

        // 2026-07-25 추가: 지도에서 정밀 배치 시 위경도. 이게 없으면(=null)
        // SIM이 zoneId 구역의 대표점으로 근사한다. 예전엔 이 필드가 없어서
        // 프론트가 정밀 좌표를 보내도 여기서 조용히 유실되고 있었음.
        Double latitude,
        Double longitude
) {
}