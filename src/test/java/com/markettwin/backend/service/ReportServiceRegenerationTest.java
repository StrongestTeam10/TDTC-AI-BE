package com.markettwin.backend.service;

import com.markettwin.backend.client.SimulationEngineClient;
import com.markettwin.backend.domain.entity.Baseline;
import com.markettwin.backend.domain.entity.BaselineResult;
import com.markettwin.backend.domain.entity.Market;
import com.markettwin.backend.domain.entity.Scenario;
import com.markettwin.backend.domain.entity.ScenarioResult;
import com.markettwin.backend.domain.entity.User;
import com.markettwin.backend.dto.request.ReportGenerateRequestDto;
import com.markettwin.backend.repository.BaselineRepository;
import com.markettwin.backend.repository.BaselineResultRepository;
import com.markettwin.backend.repository.MarketRepository;
import com.markettwin.backend.repository.ReportQueryRepository;
import com.markettwin.backend.repository.ScenarioRepository;
import com.markettwin.backend.repository.ScenarioResultRepository;
import com.markettwin.backend.repository.ZoneRepository;
import com.markettwin.backend.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 보고서 재생성 시의 뒷정리와 비교 기준 기록을 확인한다.
 *
 * 두 가지가 대상이다.
 *   - 이전 S3 객체를 지워 참조 없는 파일이 쌓이지 않는지
 *   - 어느 현행안 결과와 비교했는지 결과 행에 남기는지
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReportServiceRegenerationTest {

    private static final Long SCENARIO_ID = 48L;
    private static final Long RESULT_ID = 200L;
    private static final Long MARKET_ID = 1L;
    private static final Long BASELINE_RESULT_ID = 7L;
    private static final Long OWNER_ID = 3L;
    private static final String OLD_KEY = "reports/old-object";
    private static final String NEW_KEY = "reports/new-object";

    @Mock private ScenarioRepository scenarioRepository;
    @Mock private ScenarioResultRepository scenarioResultRepository;
    @Mock private BaselineRepository baselineRepository;
    @Mock private BaselineResultRepository baselineResultRepository;
    @Mock private MarketRepository marketRepository;
    @Mock private ZoneRepository zoneRepository;
    @Mock private ReportQueryRepository reportQueryRepository;
    @Mock private ScenarioDisplayNameResolver scenarioDisplayNameResolver;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private SimulationEngineClient simulationEngineClient;
    @Mock private FileStorageService fileStorageService;

    @InjectMocks
    private ReportService reportService;

    /** 이 시나리오 결과에 이미 붙어 있는 보고서 키. null이면 최초 생성이다. */
    private void givenExistingReport(String storageKey) throws Exception {
        ScenarioResult result = ScenarioResult.builder()
                .resultId(RESULT_ID)
                .scenarioId(SCENARIO_ID)
                .predictedDensity(BigDecimal.valueOf(0.5))
                .generatedReportPath(storageKey)
                .executedAt(Instant.parse("2026-08-04T00:00:00Z"))
                .build();

        given(scenarioRepository.findById(SCENARIO_ID)).willReturn(
                Optional.of(Scenario.builder()
                        .scenarioId(SCENARIO_ID)
                        .userId(OWNER_ID)
                        .marketId(MARKET_ID)
                        .agentCount(100)
                        .policyTypeCode("POLFR")
                        .regDatetime(Instant.parse("2026-08-04T00:00:00Z"))
                        .build()));
        given(scenarioResultRepository.findByScenarioId(SCENARIO_ID))
                .willReturn(List.of(result));
        given(marketRepository.findById(MARKET_ID)).willReturn(
                Optional.of(Market.builder()
                        .marketId(MARKET_ID)
                        .marketName("망원시장")
                        .build()));
        given(baselineRepository
                .findFirstByMarketIdAndIsActiveTrueOrderByBaselineIdDesc(MARKET_ID))
                .willReturn(Optional.of(Baseline.builder()
                        .baselineId(1L)
                        .marketId(MARKET_ID)
                        .baselineName("현행 운영안")
                        .build()));
        given(baselineResultRepository
                .findFirstByBaselineIdOrderByExecutedAtDescBaselineResultIdDesc(1L))
                .willReturn(Optional.of(BaselineResult.builder()
                        .baselineResultId(BASELINE_RESULT_ID)
                        .baselineId(1L)
                        .agentCount(100)
                        .predictedDensity(BigDecimal.valueOf(0.4))
                        .executedAt(Instant.parse("2026-08-03T00:00:00Z"))
                        .build()));
        given(zoneRepository.findByMarketId(MARKET_ID)).willReturn(List.of());
        given(simulationEngineClient.generateReportDocx(any())).willReturn(
                new SimulationEngineClient.GeneratedReport(
                        new byte[]{1, 2, 3}, "망원시장 화재 대응 보고서"));
        given(fileStorageService.uploadReport(any(), anyString(), anyString()))
                .willReturn(NEW_KEY);
        given(fileStorageService.generateReportDownloadUrl(anyString(), anyString(), any()))
                .willReturn(URI.create("https://example.test/report").toURL());
    }

    @BeforeEach
    void setUpCommonStubs() {
        given(scenarioDisplayNameResolver.resolve(any(), any(), any(), any()))
                .willReturn("망원시장 화재 시나리오");

        // 소유자 검증(ReportService.assertOwner)을 통과시키기 위한 최소 설정.
        // 검증 자체는 ReportServiceOwnerCheckTest가 다루므로 여기서는 본인 시나리오로 둔다.
        given(currentUserProvider.getCurrentUser()).willReturn(
                User.builder()
                        .userId(OWNER_ID)
                        .loginId("owner")
                        .rulesCode("ROL03")
                        .build());
    }

    @Test
    @DisplayName("재생성하면 이전 보고서 객체를 S3에서 지운다")
    void deletesPreviousReportObject() throws Exception {
        givenExistingReport(OLD_KEY);

        reportService.generate(new ReportGenerateRequestDto(SCENARIO_ID, null, null));

        verify(fileStorageService).deleteReport(OLD_KEY);
    }

    @Test
    @DisplayName("최초 생성이면 지울 이전 객체가 없다")
    void skipsDeletionOnFirstGeneration() throws Exception {
        givenExistingReport(null);

        reportService.generate(new ReportGenerateRequestDto(SCENARIO_ID, null, null));

        verify(fileStorageService, never()).deleteReport(anyString());
    }

    @Test
    @DisplayName("이전 키와 새 키가 같으면 방금 올린 객체를 지우지 않는다")
    void doesNotDeleteWhenKeyUnchanged() throws Exception {
        givenExistingReport(NEW_KEY);

        reportService.generate(new ReportGenerateRequestDto(SCENARIO_ID, null, null));

        verify(fileStorageService, never()).deleteReport(anyString());
    }

    @Test
    @DisplayName("비교에 쓴 현행안 결과 id를 결과 행에 기록한다")
    void recordsBaselineResultId() throws Exception {
        givenExistingReport(OLD_KEY);

        reportService.generate(new ReportGenerateRequestDto(SCENARIO_ID, null, null));

        verify(reportQueryRepository).updateReportInfo(
                eq(RESULT_ID), eq(NEW_KEY), anyString(), eq(BASELINE_RESULT_ID));
    }
}
