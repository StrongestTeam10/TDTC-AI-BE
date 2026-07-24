package com.markettwin.backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * 2026-07-24 추가: 실측 상태에서 출발한 예측 시뮬레이션 요청. SIM PredictRequest와 1:1 매칭.
 *
 * 파이프라인 B(ScenarioRequestDto)와 달리 화재 등 외부 충격 이벤트를 다루지 않는다.
 * 실제 관측된 인원 배치를 초기 상태로 삼아, 매대(오브젝트) 매력도 기반 자연스러운
 * 이동과 게이트를 통한 신규 유입만으로 "인구가 몰렸을 때" 위험도가 어떻게
 * 전개되는지를 본다.
 */
public record PredictRequestDto(
        @NotNull
        Long marketId,

        // null이면 SIM이 각 구역의 최신 관측값을 예측 출발점으로 사용
        Instant capturedAt,

        @Min(1) @Max(1000)
        Integer steps,

        // 전체 시뮬레이션 동안 게이트로 유입될 총 인원수. 스텝마다 무작위 인원이
        // 유입되고 합계가 이 값에 맞춰짐(스텝당 고정 인원이 아님). 0이면 신규 유입 없음.
        // (2026-07-24: inflowPerStep에서 totalInflow로 변경 — 고정 인원/스텝 대신
        // 총량을 지정하면 랜덤 분산되도록 재설계)
        @Min(0) @Max(100_000)
        Integer totalInflow,

        Integer seed
) {
}
