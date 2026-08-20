package com.markettwin.backend.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * 화재/음향 이상 이벤트. SIM의 EventTrigger와 1:1 매칭.
 * 오브젝트 배치와 같은 방식(지도 클릭 -> zoneId + 위경도 + intensity)으로 배치한다.
 *
 * triggerStep. 이 이벤트가 실제로 발동하는 스텝 번호(1부터
 * 시작). FE가 값을 안 보내거나 null을 보내도 SIM 쪽 필수 필드(기본값 1)와
 * 어긋나지 않도록, 컴팩트 생성자에서 null이면 1로 채워 넣는다.
 *
 * 화재 생애주기. burnSteps 동안 연소 후 진압되고, recoverySteps
 * 동안 위험도가 서서히 복구된다. SIM EventTrigger의 기본값(18/12)과 맞춰,
 * null이면 컴팩트 생성자에서 채워 SIM 필수 int 필드에 null이 전달되지 않게 한다.
 */
public record EventTriggerDto(
        @NotNull
        // "acoustic_anomaly"를 지웠다. 음향 이상 이벤트는 SIM·FE에서 이미
        // 제거되어(SIM EventTrigger.eventType은 "fire"만 받는다) 값으로 올 수 없다.
        String eventType,   // "fire"

        @NotNull
        Long zoneId,

        @DecimalMin("0.0") @DecimalMax("1.0")
        Double intensity,

        Double latitude,
        Double longitude,

        Integer triggerStep,

        Integer burnSteps,
        Integer recoverySteps
) {
        public EventTriggerDto {
                if (triggerStep == null) {
                        triggerStep = 1;
                }
                if (burnSteps == null) {
                        burnSteps = 18;
                }
                if (recoverySteps == null) {
                        recoverySteps = 12;
                }
        }
}