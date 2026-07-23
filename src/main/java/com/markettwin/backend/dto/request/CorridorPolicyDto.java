package com.markettwin.backend.dto.request;

import jakarta.validation.constraints.NotNull;

/** SIM의 CorridorPolicy와 1:1 매칭. 구역 간 통로에 대한 정책. */
public record CorridorPolicyDto(
        @NotNull
        Long fromZoneId,

        @NotNull
        Long toZoneId,

        @NotNull
        String action,          // "close" | "open" | "one_way"

        String allowedDirection // "from_to" | "to_from" (action이 one_way일 때만)
) {
}