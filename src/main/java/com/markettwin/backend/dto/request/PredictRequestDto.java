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
 * 전개되는지를 본다.
 *
 * 2026-08-12 변경: 오브젝트 배치/통로정책도 Before가 받는다. 시장 구조 등록에서
 * 저장한 "현행"(mrkobjt01m)을 개입 전(Before)에도 그대로 반영해야 개입 전이 실제
 * 시장 모습이 되고, 개입 후와의 차이가 순수하게 "사용자 개입분"이 된다. (게이트
 * 폐쇄는 이전부터 Before가 받고 있었다 - closedGateIds.) After는 이 현행에 사용자가
 * 삭제/추가한 결과를 ScenarioRequestDto로 보낸다.
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

        // 2026-08-12 추가: 현행 오브젝트/통로정책(시장 구조 등록에서 저장한 것).
        // ScenarioRequestDto와 동일 형식이라 그대로 SIM에 전달된다. null이면 빈 목록.
        @Valid
        List<PlacedObjectDto> objects,

        @Valid
        List<CorridorPolicyDto> corridorPolicies,

        // 2026-08-12 추가: CCTV 관측 초기배치. BE가 현재 프레임 픽셀좌표를 구역 폴리곤에
        // 비례매핑해 채운다(FE는 안 보냄). null이면 빈 목록 → SIM은 유입만으로 배치.
        List<ObservedAgentDto> observedAgents,

        Integer seed
) {
        public PredictRequestDto {
                if (closedGateIds == null) {
                        closedGateIds = List.of();
                }
                if (objects == null) {
                        objects = List.of();
                }
                if (corridorPolicies == null) {
                        corridorPolicies = List.of();
                }
                if (observedAgents == null) {
                        observedAgents = List.of();
                }
        }

        /** BE가 관측 좌표를 계산한 뒤 그 값으로 채운 새 요청을 만든다(레코드 불변). */
        public PredictRequestDto withObservedAgents(List<ObservedAgentDto> agents) {
                return new PredictRequestDto(
                        marketId, capturedAt, steps, totalInflow, events,
                        closedGateIds, objects, corridorPolicies, agents, seed);
        }
}