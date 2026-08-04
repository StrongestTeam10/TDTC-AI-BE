package com.markettwin.backend.service;

import com.markettwin.backend.client.SimulationEngineClient;
import com.markettwin.backend.domain.entity.Baseline;
import com.markettwin.backend.domain.entity.BaselineResult;
import com.markettwin.backend.domain.entity.Market;
import com.markettwin.backend.domain.entity.Scenario;
import com.markettwin.backend.domain.entity.ScenarioResult;
import com.markettwin.backend.domain.entity.User;
import com.markettwin.backend.dto.request.*;
import com.markettwin.backend.dto.response.ReportDownloadDto;
import com.markettwin.backend.dto.response.ReportGenerateResponseDto;
import com.markettwin.backend.dto.response.ScenarioHistoryDto;
import com.markettwin.backend.exception.ForbiddenActionException;
import com.markettwin.backend.exception.ReportDataException;
import com.markettwin.backend.repository.BaselineRepository;
import com.markettwin.backend.repository.BaselineResultRepository;
import com.markettwin.backend.repository.MarketRepository;
import com.markettwin.backend.repository.ReportQueryRepository;
import com.markettwin.backend.repository.ScenarioRepository;
import com.markettwin.backend.repository.ScenarioResultRepository;
import com.markettwin.backend.repository.ZoneRepository;
import com.markettwin.backend.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 2026-07-27 추가 (보고서 기능)
 *
 * 시뮬레이션 결과를 현행안과 비교한 정책 보고서(DOCX)를 만든다.
 * 보고서 1건 = 현행안 1건 vs 시나리오 1건이며, 결과 행에 S3 키와 제목을 남겨
 * 별도의 보고서 테이블 없이 이력을 관리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final String KEY_PREFIX = "reports";
    /** 관리자(ROL01)는 시나리오 소유자가 아니어도 보고서를 다룰 수 있다. PostService와 같은 기준. */
    private static final String ADMIN_ROLE_CODE = "ROL01";
    /** simrslt01d.report_title 컬럼 길이. 이 값을 넘으면 저장이 실패한다. */
    private static final int REPORT_TITLE_MAX_LENGTH = 200;
    private static final Duration DOWNLOAD_URL_TTL = Duration.ofMinutes(10);
    private static final String DOCX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final DateTimeFormatter REPORT_ID_TIME =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                    .withZone(ZoneId.of("Asia/Seoul"));
    private static final DateTimeFormatter FILE_NAME_DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd")
                    .withZone(ZoneId.of("Asia/Seoul"));

    private final ScenarioRepository scenarioRepository;
    private final ScenarioResultRepository scenarioResultRepository;
    private final BaselineRepository baselineRepository;
    private final BaselineResultRepository baselineResultRepository;
    private final MarketRepository marketRepository;
    private final ZoneRepository zoneRepository;
    private final ReportQueryRepository reportQueryRepository;
    private final ScenarioDisplayNameResolver scenarioDisplayNameResolver;
    private final CurrentUserProvider currentUserProvider;
    private final SimulationEngineClient simulationEngineClient;
    private final FileStorageService fileStorageService;

    /**
     * 보고서 식별자를 만든다.
     *
     * SIM이 이 값을 출력 폴더명·파일명으로 그대로 쓰므로 파일시스템에 안전한 ASCII만 쓴다.
     * 같은 시장에서 1초 안에 두 번 생성될 때 폴더가 겹쳐 이전 보고서를 덮어쓰는 것을
     * 막기 위해 짧은 난수를 덧붙였다.
     */
    private String newReportId(Long marketId) {
        String suffix = UUID.randomUUID().toString()
                .substring(0, 4).toUpperCase();
        return "RPT-MKT%d-%s-%s".formatted(
                marketId, REPORT_ID_TIME.format(Instant.now()), suffix);
    }

    /**
     * {@code @Transactional}을 일부러 붙이지 않았다.
     *
     * SIM 호출은 RAG 검색 + LLM 생성 + 차트 렌더까지 포함해 수 분이 걸릴 수 있다.
     * 트랜잭션으로 감싸면 그동안 DB 커넥션을 붙잡고 있어서, 동시 요청이 몇 건만 겹쳐도
     * 커넥션 풀이 마른다. 조회 → 외부 호출 → 갱신 순서로 끊어 실행한다.
     */
    public ReportGenerateResponseDto generate(ReportGenerateRequestDto request) {
        Scenario scenario = scenarioRepository.findById(request.scenarioId())
                .orElseThrow(() -> new ReportDataException(
                        "시나리오를 찾을 수 없습니다: scenarioId=" + request.scenarioId()));
        assertOwner(scenario);

        ScenarioResult scenarioResult = latestResultOf(scenario.getScenarioId());
        Market market = loadMarket(scenario);

        // 비교 기준: 같은 시장의 현재 유효한 현행안과 그 최신 결과
        Baseline baseline = loadBaseline(market);
        BaselineResult baselineResult = loadBaselineResult(baseline, market);

        String reportId = newReportId(market.getMarketId());
        ReportBundleDto bundle = toBundle(
                reportId, request, market, baseline, baselineResult, scenario, scenarioResult);

        SimulationEngineClient.GeneratedReport report =
                simulationEngineClient.generateReportDocx(bundle);
        log.info("보고서 생성 완료: reportId={}, scenarioId={}, title={}, {}bytes",
                reportId, scenario.getScenarioId(), report.title(), report.docx().length);

        String storageKey =
                fileStorageService.uploadReport(report.docx(), DOCX_CONTENT_TYPE, KEY_PREFIX);

        // 보고서 1건 = 시나리오 1건이므로 결과 행에 S3 키와 제목을 그대로 남긴다.
        // 같은 시나리오로 다시 생성하면 최신 값으로 덮이고, 이전 S3 객체는 참조를 잃는다.
        // 제목은 목록 표시용이며, 문서 표지와 같은 값이라야 사용자가 혼동하지 않는다.
        reportQueryRepository.updateReportInfo(
                scenarioResult.getResultId(), storageKey, fitReportTitle(report.title()));

        String downloadUrl = fileStorageService
                .generateReportDownloadUrl(storageKey, reportId + ".docx", DOWNLOAD_URL_TTL)
                .toString();

        return new ReportGenerateResponseDto(
                reportId, scenario.getScenarioId(), downloadUrl, storageKey);
    }

    // ------------------------------------------------------------------
    // 조회 API
    // ------------------------------------------------------------------

    /**
     * 로그인한 사용자가 실행한 시뮬레이션 이력을 최신순으로 돌려준다.
     *
     * 보고서가 없는 실행도 함께 나온다. 사용자가 보려는 것은 실행 이력이고 보고서는
     * 그중 일부에 붙은 부가 정보이므로, 목록의 대표 이름은 시나리오명을 쓴다.
     */
    public List<ScenarioHistoryDto> listMyScenarios(Long userId) {
        return reportQueryRepository.findScenarioHistoryByUserId(userId).stream()
                .map(row -> {
                    boolean hasReport = row.getStorageKey() != null
                            && !row.getStorageKey().isBlank();
                    return new ScenarioHistoryDto(
                            row.getScenarioId(),
                            scenarioDisplayNameResolver.resolve(
                                    row.getScenarioName(),
                                    row.getMarketName(),
                                    row.getPolicyTypeCode(),
                                    row.getRegDatetime()),
                            row.getMarketId(),
                            row.getMarketName(),
                            row.getAgentCount(),
                            row.getPolicyTypeCode(),
                            row.getExecutedAt(),
                            hasReport,
                            hasReport ? row.getReportTitle() : null,
                            hasReport
                                    ? "/api/simulation/reports/" + row.getScenarioId() + "/download"
                                    : null);
                })
                .toList();
    }

    /**
     * 이미 만들어 둔 보고서의 다운로드 주소를 새로 발급한다.
     *
     * presigned URL은 수명이 짧아 DB에 담을 수 없으므로 S3 키만 저장해 두고, 필요할 때
     * 이렇게 주소만 다시 만든다. 파일은 S3에 그대로 있으니 보고서를 재생성할 필요가 없다.
     * (재생성은 RAG 검색 + LLM 생성까지 다시 도는 수 분짜리 작업이다)
     */
    public ReportDownloadDto reissueDownloadUrl(Long scenarioId) {
        // 소유자 확인을 보고서 존재 여부보다 먼저 한다. 순서가 반대면 남의 시나리오라도
        // "생성된 보고서가 없습니다"와 다른 오류를 구분해 보고서 유무를 알아낼 수 있다.
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new ReportDataException(
                        "시나리오를 찾을 수 없습니다: scenarioId=" + scenarioId));
        assertOwner(scenario);

        ScenarioResult result = latestResultOf(scenarioId);

        String storageKey = result.getGeneratedReportPath();
        if (storageKey == null || storageKey.isBlank()) {
            throw new ReportDataException(
                    "생성된 보고서가 없습니다: scenarioId=" + scenarioId
                            + ". 먼저 보고서를 생성해야 다운로드할 수 있습니다.");
        }

        URL url = fileStorageService.generateReportDownloadUrl(
                storageKey, downloadFileName(scenario), DOWNLOAD_URL_TTL);
        return new ReportDownloadDto(
                scenarioId, url.toString(), DOWNLOAD_URL_TTL.toSeconds());
    }

    /**
     * 이 시나리오를 다룰 수 있는 사용자인지 확인한다.
     *
     * 인증만 통과하면 남의 scenarioId로 보고서를 생성하거나 내려받을 수 있었기에 추가했다.
     * 관리자(ROL01)는 통과시키며, 이는 게시판(PostService.assertCanModify)과 같은 기준이다.
     *
     * user_id가 NULL인 행은 소유자를 알 수 없으므로 거부한다. 시나리오 저장 시 user_id를
     * 채우기 전에 만들어진 데이터가 해당하며, 관리자는 위 규칙으로 여전히 접근할 수 있다.
     * "존재하지 않음"이 아니라 "권한 없음"으로 답하는 이유는, 시나리오 ID가 순번이라
     * 숨겨도 실익이 없고 담당자가 원인을 알 수 있어야 하기 때문이다.
     */
    private void assertOwner(Scenario scenario) {
        User currentUser = currentUserProvider.getCurrentUser();
        if (ADMIN_ROLE_CODE.equals(currentUser.getRulesCode())) {
            return;
        }
        if (scenario.getUserId() == null) {
            throw new ForbiddenActionException(
                    "실행자 정보가 없는 시나리오입니다. 관리자에게 문의하세요: scenarioId="
                            + scenario.getScenarioId());
        }
        if (!scenario.getUserId().equals(currentUser.getUserId())) {
            throw new ForbiddenActionException(
                    "본인이 실행한 시뮬레이션의 보고서만 생성·조회할 수 있습니다.");
        }
    }

    /**
     * 다운로드 시 브라우저가 저장할 파일명.
     *
     * 생성 당시의 reportId는 DB에 남기지 않으므로(S3 키만 저장) 시장명과 시나리오 번호로
     * 다시 만든다. 시나리오명은 자동 생성된 타임스탬프 문자열이라 파일명에 쓰기 부적합하다.
     */
    private String downloadFileName(Scenario scenario) {
        String marketName = marketRepository.findById(scenario.getMarketId())
                .map(Market::getMarketName)
                .orElse("market" + scenario.getMarketId());
        String date = FILE_NAME_DATE.format(scenario.getRegDatetime());
        return "%s_시나리오%d_%s.docx".formatted(marketName, scenario.getScenarioId(), date);
    }

    // ------------------------------------------------------------------
    // 조회 — SIM이 400으로 거절할 조건을 미리 걸러 원인을 알려준다
    // ------------------------------------------------------------------

    /**
     * 시나리오의 최신 결과 1건.
     *
     * 정렬 기준(executed_at → result_id)은 SIM report_adapter의
     * _latest_result_by_scenario() 및 BaselineResultRepository의 조회와 동일하게 맞췄다.
     * 한쪽만 바꾸면 기준안과 대안이 서로 다른 규칙으로 뽑혀 결과가 어긋난다.
     */
    private ScenarioResult latestResultOf(Long scenarioId) {
        return scenarioResultRepository.findByScenarioId(scenarioId).stream()
                .max(Comparator.comparing(ScenarioResult::getExecutedAt)
                        .thenComparing(ScenarioResult::getResultId))
                .orElseThrow(() -> new ReportDataException(
                        "시뮬레이션 결과가 없는 시나리오입니다: scenarioId=" + scenarioId));
    }

    private Market loadMarket(Scenario scenario) {
        Long marketId = scenario.getMarketId();
        if (marketId == null) {
            throw new ReportDataException(
                    "시나리오에 market_id가 없습니다: scenarioId=" + scenario.getScenarioId());
        }
        return marketRepository.findById(marketId)
                .orElseThrow(() -> new ReportDataException(
                        "시장 정보를 찾을 수 없습니다: marketId=" + marketId));
    }

    private Baseline loadBaseline(Market market) {
        return baselineRepository
                .findFirstByMarketIdAndIsActiveTrueOrderByBaselineIdDesc(market.getMarketId())
                .orElseThrow(() -> new ReportDataException(
                        "비교할 현행안이 없습니다: marketId=" + market.getMarketId()
                                + " (" + market.getMarketName() + "). "
                                + "simbsln01m에 이 시장의 현행안을 등록해야 보고서를 만들 수 있습니다."));
    }

    /**
     * 비교 기준으로 쓸 현행안 결과(최신 1건)를 찾는다.
     *
     * 보고서는 "시나리오 1건 vs 그 시장의 현행안"이므로 인구수로 좁히지 않는다.
     * 인구수가 서로 다를 수 있다는 사실은 SIM이 보고서에 시나리오별 투입 인구를
     * 표시하고 분석 가정에 명시하는 방식으로 다룬다.
     */
    private BaselineResult loadBaselineResult(Baseline baseline, Market market) {
        return baselineResultRepository
                .findFirstByBaselineIdOrderByExecutedAtDescBaselineResultIdDesc(
                        baseline.getBaselineId())
                .orElseThrow(() -> new ReportDataException(
                        "현행안 실행 결과가 없습니다: marketId=" + market.getMarketId()
                                + ", baselineId=" + baseline.getBaselineId() + ". "
                                + "현행안을 한 번이라도 실행해 simbsln01d에 결과가 있어야 "
                                + "보고서를 만들 수 있습니다."));
    }

    // ------------------------------------------------------------------
    // 조립
    // ------------------------------------------------------------------

    private ReportBundleDto toBundle(
            String reportId,
            ReportGenerateRequestDto request,
            Market market,
            Baseline baseline,
            BaselineResult baselineResult,
            Scenario scenario,
            ScenarioResult scenarioResult) {

        return new ReportBundleDto(
                new ReportMetaDto(
                        reportId,
                        request.reportTitle(),
                        request.decisionQuestion()),
                new MarketInfoDto(
                        market.getMarketId(),
                        market.getMarketName(),
                        market.getLatitude(),
                        market.getLongitude()),
                // 개입 문장을 "1구역" 대신 "남측 구역"으로 쓰기 위해 구역 이름을 함께 보낸다.
                zoneRepository.findByMarketId(market.getMarketId()).stream()
                        .map(zone -> new ZoneInfoDto(zone.getZoneId(), zone.getZoneName()))
                        .toList(),
                toBaselineRow(baseline, baselineResult, market),
                toBaselineResultRow(baseline, baselineResult),
                List.of(toScenarioRow(scenario, market)),
                List.of(toResultRow(scenarioResult)));
    }

    /**
     * 현행안을 시나리오 한 행처럼 변환한다.
     *
     * scenario_id 자리에는 baseline_id를 넣는다. SIM은 기준안을 별도 필드로 받으므로
     * 대안의 scenario_id와 값이 같아도 충돌하지 않는다.
     */
    private ScenarioRowDto toBaselineRow(
            Baseline baseline, BaselineResult baselineResult, Market market) {

        String name = (baseline.getBaselineName() == null
                || baseline.getBaselineName().isBlank())
                ? market.getMarketName() + " 현행 운영안"
                : baseline.getBaselineName();

        return new ScenarioRowDto(
                baseline.getBaselineId(),
                name,
                baseline.getMarketId(),
                blankToEmptyJson(baseline.getVirtualConfig()),
                baseline.getRegDatetime(),
                baselineResult.getAgentCount(),   // 인구수는 결과 쪽에 있다
                baseline.getPolicyTypeCode(),
                baseline.getCreatedAt());
    }

    private ScenarioResultRowDto toBaselineResultRow(
            Baseline baseline, BaselineResult result) {

        return new ScenarioResultRowDto(
                result.getBaselineResultId(),
                baseline.getBaselineId(),        // 위 toBaselineRow의 scenario_id와 맞춰야 한다
                result.getPredictedMaxDensity(),
                result.getPredictedDensity(),
                result.getPredictedRiskScore(),
                null,   // economic_effect_analysis: 현행안에는 없는 개념
                null,   // generated_report_path: 여러 보고서에 재사용되므로 두지 않음
                null,   // avg_stay_time: 현행안 결과 테이블에 없음
                null,   // flow_direction: 현행안 결과 테이블에 없음
                result.getExecutedAt());
    }

    /**
     * 시나리오명은 표시용으로 바꿔 넣는다.
     * 이 값이 보고서 문서의 시나리오 구성표·결과 비교표에 그대로 실리므로,
     * 자동 생성된 타임스탬프 문자열이 정책 보고서에 남지 않도록 한다.
     */
    private ScenarioRowDto toScenarioRow(Scenario scenario, Market market) {
        return new ScenarioRowDto(
                scenario.getScenarioId(),
                scenarioDisplayNameResolver.resolve(
                        scenario.getScenarioName(),
                        market.getMarketName(),
                        scenario.getPolicyTypeCode(),
                        scenario.getRegDatetime()),
                scenario.getMarketId(),
                // SIM은 null을 허용하지 않고 dict/str만 받는다. 빈 JSON으로 보정.
                blankToEmptyJson(scenario.getVirtualConfig()),
                scenario.getRegDatetime(),
                scenario.getAgentCount(),
                scenario.getPolicyTypeCode(),
                scenario.getCreatedAt());
    }

    private ScenarioResultRowDto toResultRow(ScenarioResult result) {
        return new ScenarioResultRowDto(
                result.getResultId(),
                result.getScenarioId(),
                result.getPredictedMaxDensity(),
                result.getPredictedDensity(),
                result.getPredictedRiskScore(),
                result.getEconomicEffectAnalysis(),
                result.getGeneratedReportPath(),
                // Duration -> "PT30M". null이면 SIM에서 체류시간 항목이 빠질 뿐 정상 생성된다.
                result.getAvgStayTime() == null ? null : result.getAvgStayTime().toString(),
                result.getFlowDirection(),
                result.getExecutedAt());
    }

    /**
     * 제목을 simrslt01d.report_title(VARCHAR(200)) 길이에 맞춘다.
     *
     * 요청 DTO에도 @Size(max = 200)이 걸려 있지만, 제목은 SIM이 LLM으로 다듬은 값이라
     * 요청과 무관하게 길어질 수 있다. 여기서 막지 않으면 S3 업로드가 끝난 뒤 DB 갱신에서
     * 실패해 참조 없는 파일만 남는다. 문서 자체는 이미 만들어졌으므로 저장을 실패시키기보다
     * 목록 표기를 줄이는 편이 낫다고 판단했다.
     */
    private String fitReportTitle(String title) {
        if (title == null) {
            return null;
        }
        String trimmed = title.trim();
        if (trimmed.length() <= REPORT_TITLE_MAX_LENGTH) {
            return trimmed;
        }
        log.warn("보고서 제목이 저장 한도를 넘어 잘랐습니다: {}자 -> {}자",
                trimmed.length(), REPORT_TITLE_MAX_LENGTH);
        return trimmed.substring(0, REPORT_TITLE_MAX_LENGTH);
    }

    private String blankToEmptyJson(String value) {
        return (value == null || value.isBlank()) ? "{}" : value;
    }
}
