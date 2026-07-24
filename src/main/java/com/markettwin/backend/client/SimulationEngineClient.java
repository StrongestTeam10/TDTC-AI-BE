package com.markettwin.backend.client;

import com.markettwin.backend.dto.request.PredictRequestDto;
import com.markettwin.backend.dto.request.ScenarioRequestDto;
import com.markettwin.backend.dto.request.SnapshotRequestDto;
import com.markettwin.backend.dto.response.DashboardSnapshotDto;
import com.markettwin.backend.dto.response.PredictResultDto;
import com.markettwin.backend.dto.response.ScenarioResultDto;
import com.markettwin.backend.exception.SimulationEngineException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * FastAPI로 감싼 Mesa 시뮬레이션 엔진(별도 Python 마이크로서비스) 호출 클라이언트.
 * Spring Boot는 이 서비스를 내부 네트워크(VPC)에서만 호출하고 외부에는 노출하지 않는다.
 */
@Component
@RequiredArgsConstructor
public class SimulationEngineClient {

    private final WebClient simulationEngineWebClient;

    public ScenarioResultDto runScenario(ScenarioRequestDto request) {
        return simulationEngineWebClient.post()
                .uri("/simulate/scenario")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ScenarioResultDto.class)
                .timeout(Duration.ofSeconds(30))
                .onErrorMap(ex -> new SimulationEngineException(
                        "시뮬레이션 엔진 호출 실패: " + ex.getMessage(), ex))
                .block();
    }

    public DashboardSnapshotDto getSnapshot(SnapshotRequestDto request) {
        return simulationEngineWebClient.post()
                .uri("/simulate/snapshot")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(DashboardSnapshotDto.class)
                .timeout(Duration.ofSeconds(30))
                .onErrorMap(ex -> new SimulationEngineException(
                        "시뮬레이션 엔진 호출 실패: " + ex.getMessage(), ex))
                .block();
    }

    public PredictResultDto predict(PredictRequestDto request) {
        return simulationEngineWebClient.post()
                .uri("/simulate/predict")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PredictResultDto.class)
                .timeout(Duration.ofSeconds(60))
                .onErrorMap(ex -> new SimulationEngineException(
                        "시뮬레이션 엔진 호출 실패: " + ex.getMessage(), ex))
                .block();
    }

    public Mono<Boolean> healthCheck() {
        return simulationEngineWebClient.get()
                .uri("/health")
                .retrieve()
                .toBodilessEntity()
                .map(response -> response.getStatusCode().is2xxSuccessful())
                .onErrorReturn(false);
    }
}
