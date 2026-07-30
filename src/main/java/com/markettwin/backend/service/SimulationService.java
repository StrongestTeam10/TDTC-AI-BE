package com.markettwin.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.markettwin.backend.client.SimulationEngineClient;
import com.markettwin.backend.domain.entity.Scenario;
import com.markettwin.backend.domain.entity.ScenarioResult;
import com.markettwin.backend.dto.request.EventTriggerDto;
import com.markettwin.backend.dto.request.PredictRequestDto;
import com.markettwin.backend.dto.request.ScenarioRequestDto;
import com.markettwin.backend.dto.response.PredictResultDto;
import com.markettwin.backend.dto.response.ScenarioResultDto;
import com.markettwin.backend.repository.ScenarioRepository;
import com.markettwin.backend.repository.ScenarioResultRepository;
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
 */
@Service
@RequiredArgsConstructor
public class SimulationService {

    private final SimulationEngineClient simulationEngineClient;
    private final ScenarioRepository scenarioRepository;
    private final ScenarioResultRepository scenarioResultRepository;
    private final ObjectMapper objectMapper;

    public ScenarioResultDto runScenario(ScenarioRequestDto request) {
        Scenario scenario = saveScenario(request);
        ScenarioResultDto result = simulationEngineClient.runScenario(request);
        saveScenarioResult(scenario.getScenarioId(), result);
        return result;
    }

    private Scenario saveScenario(ScenarioRequestDto request) {
        String virtualConfig = toJson(request);
        String spaceModData = toJson(request.objects());

        Scenario scenario = Scenario.builder()
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

    private String derivePolicyTypeCode(ScenarioRequestDto request) {
        boolean hasFire = request.events() != null && request.events().stream()
                .anyMatch(e -> "fire".equals(e.eventType()));
        if (hasFire) {
            return "POLFR";
        }
        boolean hasAcoustic = request.events() != null && request.events().stream()
                .anyMatch(e -> "acoustic_anomaly".equals(e.eventType()));
        if (hasAcoustic) {
            return "POLAC";
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

    public PredictResultDto predict(PredictRequestDto request) {
        return simulationEngineClient.predict(request);
    }
}