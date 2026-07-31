package com.markettwin.backend.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * 2026-07-25 추가: 화재/음향 이상 이벤트. SIM의 EventTrigger와 1:1 매칭.
 * 오브젝트 배치와 같은 방식(지도 클릭 -> zoneId + 위경도 + intensity)으로 배치한다.
 *
 * 2026-07-29 추가: triggerStep. 이 이벤트가 실제로 발동하는 스텝 번호(1부터
 * 시작). FE가 값을 안 보내거나 null을 보내도 SIM 쪽 필수 필드(기본값 1)와
 * 어긋나지 않도록, 컴팩트 생성자에서 null이면 1로 채워 넣는다.
 */
public record EventTriggerDto(
        @NotNull
        String eventType,   // "fire" | "acoustic_anomaly"

        @NotNull
        Long zoneId,

        @DecimalMin("0.0") @DecimalMax("1.0")
        Double intensity,

        Double latitude,
        Double longitude,

        Integer triggerStep
) {
        public EventTriggerDto {
                if (triggerStep == null) {
                        triggerStep = 1;
                }
        }
}