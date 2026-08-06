package com.markettwin.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

/**
 * 2026-07-24 추가: 실측 상태에서 출발한 예측 시뮬레이션 요청. SIM PredictRequest와 1:1 매칭.
 *
 * 실제 관측된 인원 배치를 초기 상태로 삼아, 매대(오브젝트) 매력도 기반 자연스러운
 * 이동과 게이트를 통한 신규 유입만으로 "인구가 몰렸을 때" 위험도가 어떻게
 * 전개되는지를 본다. 오브젝트 배치/통로정책/게이트 폐쇄는 여전히
 * ScenarioRequestDto(파이프라인 B, After) 전용이다.
 *
 * 2026-08-XX 추가: events(화재). FE는 이전부터 Before 화면에서도 이벤트를
 * 같이 보내고 있었는데, 이 DTO에 필드가 없어 역직렬화 시 조용히 버려지고
 * 있었다(SIM PredictRequest는 이미 events를 받아 처리하고 있었음). 필드를
 * 추가해 실제로 SIM까지 전달되게 한다.
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

        @Valid
        List<EventTriggerDto> events,

        // 2026-08-XX 추가: 게이트 개폐를 개입 전/후 독립적으로 조절하므로 Before도
        // 닫힌 게이트 목록을 받아 SIM으로 전달한다. null이면 빈 목록으로 채워
        // SIM 필수 필드(default_factory=list)에 null이 전달되지 않게 한다.
        List<Long> closedGateIds,

        Integer seed
) {
        public PredictRequestDto {
                if (closedGateIds == null) {
                        closedGateIds = List.of();
                }
        }
}