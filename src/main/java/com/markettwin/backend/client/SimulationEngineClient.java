package com.markettwin.backend.client;

import com.markettwin.backend.dto.request.PredictRequestDto;
import com.markettwin.backend.dto.request.ReportBundleDto;
import com.markettwin.backend.dto.request.ScenarioRequestDto;
import com.markettwin.backend.dto.request.SnapshotRequestDto;
import com.markettwin.backend.dto.response.DashboardSnapshotDto;
import com.markettwin.backend.dto.response.PredictResultDto;
import com.markettwin.backend.dto.response.ScenarioResultDto;
import com.markettwin.backend.exception.SimulationEngineException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * FastAPI로 감싼 Mesa 시뮬레이션 엔진(별도 Python 마이크로서비스) 호출 클라이언트.
 * Spring Boot는 이 서비스를 내부 네트워크(VPC)에서만 호출하고 외부에는 노출하지 않는다.
 */
@Slf4j
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
                .timeout(Duration.ofMinutes(5))
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
                .timeout(Duration.ofMinutes(5))
                .onErrorMap(ex -> new SimulationEngineException(
                        "시뮬레이션 엔진 호출 실패: " + ex.getMessage(), ex))
                .block();
    }

    public Object analyzePolicy(String policyText, MultipartFile file) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("policyText", policyText);
        if (file != null && !file.isEmpty()) {
            builder.part("file", file.getResource());
        }

        return simulationEngineWebClient.post()
                .uri("/policy/analyze")
                .bodyValue(builder.build())
                .retrieve()
                .bodyToMono(Object.class)
                .timeout(Duration.ofSeconds(60))
                .onErrorMap(ex -> new SimulationEngineException(
                        "정책 분석(LLM) 엔진 호출 실패: " + describeError(ex), ex))
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

    /**
     * 보고서를 생성해 DOCX 바이트를 받아온다.
     *
     * SIM의 GET /simulation/reports/{id}/docx 대신 POST .../file을 쓰는 이유:
     * 전자가 주는 경로는 SIM 컨테이너의 로컬 디스크(outputs/)라 재시작하면 사라지고
     * 인스턴스가 2대 이상이면 다른 인스턴스가 못 찾는다. 파일 자체를 받아 S3에 올린다.
     *
     * 타임아웃이 3분인 것은 RAG 검색 + LLM 본문 생성 + 차트 렌더가 모두 포함되기 때문이다.
     * (기존 시뮬레이션 호출의 30~60초로는 부족하다)
     */
    public GeneratedReport generateReportDocx(ReportBundleDto bundle) {
        ResponseEntity<byte[]> response = simulationEngineWebClient.post()
                .uri("/simulation/reports/file")
                .bodyValue(bundle)
                .retrieve()
                .toEntity(byte[].class)
                .timeout(Duration.ofMinutes(3))
                .onErrorMap(ex -> new SimulationEngineException(
                        "보고서 생성 실패: " + describeError(ex), ex))
                .block();

        if (response == null || response.getBody() == null) {
            throw new SimulationEngineException(
                    "보고서 생성 실패: 응답 본문이 비어 있습니다.", null);
        }
        return new GeneratedReport(
                response.getBody(),
                decodeTitle(response.getHeaders().getFirst("X-Report-Title")));
    }

    /**
     * SIM이 만든 DOCX와 그 문서 표지에 박힌 제목.
     *
     * 제목을 함께 받는 이유는 보고서 목록에 문서와 같은 제목을 보여주기 위함이다.
     * SIM이 LLM으로 제목을 다듬는 경우가 있어 BE가 따로 조립하면 문서와 어긋난다.
     */
    public record GeneratedReport(byte[] docx, String title) {
    }

    /**
     * SIM이 4xx/5xx로 거절했을 때 응답 본문까지 메시지에 담는다.
     *
     * WebClientResponseException의 기본 메시지는 "400 Bad Request from POST ..." 까지만
     * 담고 본문을 빼버려서, 어떤 필드가 계약을 어겼는지 알 수 없다. SIM은 거절 사유를
     * detail에 담아 보내므로 그것까지 보여야 진단이 된다.
     */
    private String describeError(Throwable ex) {
        if (ex instanceof WebClientResponseException error) {
            String body = error.getResponseBodyAsString(StandardCharsets.UTF_8);
            return body.isBlank()
                    ? error.getMessage()
                    : error.getMessage() + " / SIM 응답: " + body;
        }
        return ex.getMessage();
    }

    /** SIM이 퍼센트 인코딩해 보낸 제목을 되돌린다. 헤더가 없거나 깨지면 null. */
    private String decodeTitle(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return null;
        }
        try {
            return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            // 제목은 목록 표시용이라 실패해도 보고서 생성 자체를 막지 않는다.
            log.warn("보고서 제목 헤더를 해석하지 못했습니다: {}", encoded, e);
            return null;
        }
    }
}
