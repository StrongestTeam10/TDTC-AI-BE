package com.markettwin.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.markettwin.backend.client.SimulationEngineClient;
import com.markettwin.backend.domain.entity.Scenario;
import com.markettwin.backend.domain.entity.ScenarioResult;
import com.markettwin.backend.domain.entity.Baseline;
import com.markettwin.backend.domain.entity.BaselineResult;
import com.markettwin.backend.dto.request.EventTriggerDto;
import com.markettwin.backend.dto.request.PredictRequestDto;
import com.markettwin.backend.dto.request.ScenarioRequestDto;
import com.markettwin.backend.dto.response.PredictResultDto;
import com.markettwin.backend.dto.response.ScenarioResultDto;
import com.markettwin.backend.repository.BaselineRepository;
import com.markettwin.backend.repository.BaselineResultRepository;
import com.markettwin.backend.repository.ScenarioRepository;
import com.markettwin.backend.repository.ScenarioResultRepository;
import com.markettwin.backend.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 2026-07-27 변경: 시나리오 실행 시 DB 적재 로직 추가.
 *
 * 흐름: (1) 요청 내용을 simscnr01m에 먼저 저장해 scenarioId를 받고
 *       (2) SIM을 호출해 시뮬레이션을 돌리고
 *       (3) 결과를 simrslt01d에 scenarioId와 함께 저장한다.
 *
 * policyTypeCode(comcode01m POL 도메인: POLNO/POLFR/POLAC/POLCB)는 NOT NULL인데,
 * 지금 요청 구조(오브젝트/이벤트/통로정책을 여러 개 동시에 넣을 수 있음)와 이
 * 4개 단일 코드가 정확히 안 맞는다. 팀 논의로 코드 체계가 정리되기 전까지는
 * derivePolicyTypeCode()가 요청 내용 기준 대표값 하나를 자동 선택해 채운다
 * (화재 > 음향이상 > 통로정책 있음 > 없음 순 우선순위). 실제 상세 내용은
 * virtualConfig에 요청 전체가 JSON으로 그대로 남아있으니 정보 손실은 없다.
 *
 * 2026-08-06 변경: 실행 요청의 marketId를 그대로 믿지 않고 시장 접근 권한을 먼저
 * 확인한다(assertMarketInScope). 조회 쪽은 이미 같은 검증을 하고 있었고 실행 경로만
 * 빠져 있었다.
 *
 * user_id가 저장 로직에서 채워지지 않고 있던 문제 수정.
 * 이 API(/api/simulation/**)는 이미 인증 필수(SecurityConfig의 authenticated())라
 * 로그인된 사용자가 항상 있으므로, CurrentUserProvider(게시판 기능 때 만든 공통
 * 헬퍼)로 SecurityContext에서 현재 로그인 사용자를 조회해 userId를 채운다.
 */
@Service
@RequiredArgsConstructor
public class SimulationService {

    private final SimulationEngineClient simulationEngineClient;
    private final ScenarioRepository scenarioRepository;
    private final ScenarioResultRepository scenarioResultRepository;
    private final BaselineRepository baselineRepository;
    private final BaselineResultRepository baselineResultRepository;
    private final ObjectMapper objectMapper;
    private final CurrentUserProvider currentUserProvider;
    private final MarketService marketService;
    private final ObservedPlacementService observedPlacementService;

    public ScenarioResultDto runScenario(ScenarioRequestDto request) {
        assertMarketInScope(request.marketId());
        Scenario scenario = saveScenario(request);
        // 2026-08-12: CCTV 관측 초기배치를 조립해 SIM에 함께 보낸다(항상 적용, 데이터 없는
        // 구역은 SIM이 유입으로 채움). virtual_config엔 저장하지 않으므로 saveScenario는
        // 원본 request로 하고, 엔진 호출만 관측 좌표를 채운 요청으로 한다.
        ScenarioRequestDto effective = request.withObservedAgents(
                observedPlacementService.computeForMarket(request.marketId(), request.capturedAt()));
        ScenarioResultDto result = simulationEngineClient.runScenario(effective);
        saveScenarioResult(scenario.getScenarioId(), result);
        // 저장된 시나리오 식별자를 응답에 실어 준다. 이 값이 없으면 FE는 방금 실행한
        // 시나리오로 보고서를 만들 수 없다(SIM이 준 scenarioId는 UUID라 쓸 수 없음).
        return result.withPersistedScenarioId(scenario.getScenarioId());
    }

    private Scenario saveScenario(ScenarioRequestDto request) {
        String virtualConfig = toJson(request);
        String spaceModData = toJson(request.objects());

        Scenario scenario = Scenario.builder()
                .userId(currentUserProvider.getCurrentUser().getUserId())
                .marketId(request.marketId())
                // 2026-07-27: 시나리오 이름을 사용자가 직접 입력하는 필드가 아직
                // FE/DTO에 없어서, 실행 시각 기반으로 자동 생성한다. 나중에 FE에서
                // 이름 입력 필드가 추가되면 request.scenarioName() 같은 걸로 교체.
                .scenarioName("시나리오 " + Instant.now())
                .virtualConfig(virtualConfig)
                .spaceModData(spaceModData)
                .regDatetime(Instant.now())
                .agentCount(request.agentCount())
                .policyTypeCode(derivePolicyTypeCode(request))
                .createdAt(Instant.now())
                .build();

        return scenarioRepository.save(scenario);
    }

    /**
     * 이 실행을 대표하는 정책 유형 코드(simscnr01m.policy_type_code) 하나를 고른다.
     *
     * ⚠️ 알려진 한계(2026-08-12 확인 후 그대로 두기로 함): 컬럼이 코드 하나(VARCHAR(5))라
     * 개입을 여러 개 걸어도 아래 우선순위 중 첫 번째만 남는다. 예를 들어 화재 + 통로폐쇄를
     * 함께 실행하면 POLCB는 기록되지 않는다. 오브젝트 배치나 게이트 폐쇄만 한 실행은
     * POLNO(없음)로 남는다.
     *
     * 고치지 않는 이유: 실행 요청 원본이 simscnr01m.virtual_config에 JSON으로 통째로 남아
     * 있어 정보가 사라지지 않고, 보고서도 그 원본을 쓴다. FE도 이 값을 더는 쓰지 않는다
     * (시나리오 이력의 "정책 유형" 열과 검색 선택지를 2026-08-12에 제거했다 - 부정확한
     * 요약을 보여주고 그것으로 검색까지 하게 두는 것이 오히려 오해를 만들어서다).
     * 즉 이 값은 지금 ScenarioDisplayNameResolver가 시나리오 이름을 조립할 때만 쓴다.
     *
     * 2026-08-12: acoustic_anomaly(POLAC) 분기를 제거했다. 음향 이상 이벤트는 SIM과
     * FE에서 이미 완전히 삭제되어(SIM EventTrigger.eventType은 "fire"만 받는다) 이
     * 분기는 어떤 요청으로도 도달할 수 없는 죽은 코드였다.
     */
    private String derivePolicyTypeCode(ScenarioRequestDto request) {
        boolean hasFire = request.events() != null && request.events().stream()
                .anyMatch(e -> "fire".equals(e.eventType()));
        if (hasFire) {
            return "POLFR";
        }
        boolean hasCorridorPolicy = request.corridorPolicies() != null && !request.corridorPolicies().isEmpty();
        if (hasCorridorPolicy) {
            return "POLCB";
        }
        return "POLNO";
    }

    private void saveScenarioResult(Long scenarioId, ScenarioResultDto result) {
        ScenarioResult entity = ScenarioResult.builder()
                .scenarioId(scenarioId)
                .predictedMaxDensity(toBigDecimal(result.maxDensity()))
                .predictedDensity(toBigDecimal(result.averageDensity()))
                .predictedRiskScore(
                        result.finalRiskScore() != null
                                ? (int) Math.round(result.finalRiskScore().score())
                                : null
                )
                .executedAt(Instant.now())
                .maxDensityZoneId(result.maxDensityZoneId())
                .maxDensityZoneName(result.maxDensityZoneName())
                .evacuatedCount(result.evacuatedCount())
                .build();

        scenarioResultRepository.save(entity);
    }

    private BigDecimal toBigDecimal(Double value) {
        return value != null ? BigDecimal.valueOf(value) : null;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("시나리오 데이터 직렬화 실패", e);
        }
    }

    /**
     * 요청한 시장을 이 사용자가 다룰 수 있는지 확인한다.
     *
     * 지금까지 marketId를 그대로 믿고 실행했다. 관제요원(ROL02)은 담당 시장의 목록만
     * 내려받지만, 요청 본문의 marketId를 바꾸면 남의 시장 시뮬레이션을 실행해
     * simscnr01m에 자기 이력으로 남길 수 있었다. 조회 쪽(대시보드/구역/시설)은
     * 이미 같은 검증을 하고 있어 실행 경로만 빠져 있던 셈이다.
     *
     * 관리자(ROL01)는 MarketService 규칙대로 전체 시장을 통과한다.
     */
    private void assertMarketInScope(Long marketId) {
        marketService.getAccessibleMarket(marketId, currentUserProvider.getCurrentUser());
    }

    public PredictResultDto predict(PredictRequestDto request) {
        assertMarketInScope(request.marketId());
        // 2026-08-12: 개입 전(Before)도 개입 후와 "같은" 관측 초기배치를 쓴다. capturedAt이
        // 양쪽 동일하면 같은 프레임이 선택되어 배치가 일치한다.
        PredictRequestDto effective = request.withObservedAgents(
                observedPlacementService.computeForMarket(request.marketId(), request.capturedAt()));
        PredictResultDto result = simulationEngineClient.predict(effective);

        baselineRepository.findFirstByMarketIdAndIsActiveTrueOrderByBaselineIdDesc(request.marketId())
                .ifPresent(baseline -> saveBaselineResult(baseline.getBaselineId(), result));

        return result;
    }

    private void saveBaselineResult(Long baselineId, PredictResultDto result) {
        BaselineResult entity = BaselineResult.builder()
                .baselineId(baselineId)
                .agentCount(result.agentCount() != null ? result.agentCount() : 0)
                .predictedMaxDensity(toBigDecimal(result.maxDensity()))
                .predictedDensity(toBigDecimal(result.averageDensity()))
                .predictedRiskScore(
                        result.finalOverallRiskScore() != null
                                ? (int) Math.round(result.finalOverallRiskScore())
                                : null
                )
                .executedAt(Instant.now())
                .maxDensityZoneId(result.maxDensityZoneId())
                .maxDensityZoneName(result.maxDensityZoneName())
                .evacuatedCount(result.evacuatedCount())
                .build();

        baselineResultRepository.save(entity);
    }
}
