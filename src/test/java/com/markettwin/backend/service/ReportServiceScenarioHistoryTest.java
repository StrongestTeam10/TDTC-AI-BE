package com.markettwin.backend.service;

import com.markettwin.backend.domain.entity.User;
import com.markettwin.backend.exception.ForbiddenActionException;
import com.markettwin.backend.repository.ReportQueryRepository;
import com.markettwin.backend.security.CurrentUserProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * 시뮬레이션 이력 조회(listAllScenarios/listMyScenarios)의 권한·매핑·필터 전달을 확인한다.
 *
 * SecurityConfig의 /api/simulation/** 규칙은 ROL01·ROL02를 함께 허용하므로, 경로만으로는
 * 관제요원이 남의 이력까지 볼 수 있다. 그 한 칸을 서비스에서 좁히고 있는지가 핵심이다.
 * 거부 경로는 관리자 계정으로 재현되지 않으므로 단위 테스트로 확인한다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReportServiceScenarioHistoryTest {

    @Mock
    private ReportQueryRepository reportQueryRepository;

    @Mock
    private ScenarioDisplayNameResolver scenarioDisplayNameResolver;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private ReportService reportService;

    private void givenCurrentUser(String rulesCode) {
        given(currentUserProvider.getCurrentUser()).willReturn(
                User.builder().userId(9L).loginId("tester").rulesCode(rulesCode).build());
    }

    /** 네이티브 쿼리 프로젝션 대역. 목록 한 줄을 흉내낸다. */
    private ReportQueryRepository.ReportRow row(
            String storageKey, String ownerName, Integer riskScore) {
        return new ReportQueryRepository.ReportRow() {
            @Override public Long getScenarioId() { return 48L; }
            @Override public String getScenarioName() { return "시나리오 2026-08-06T00:00:00Z"; }
            @Override public Long getMarketId() { return 1L; }
            @Override public String getMarketName() { return "망원시장"; }
            @Override public Integer getAgentCount() { return 100; }
            @Override public String getPolicyTypeCode() { return "POLFR"; }
            @Override public Instant getRegDatetime() { return Instant.parse("2026-08-06T00:00:00Z"); }
            @Override public Instant getExecutedAt() { return Instant.parse("2026-08-06T00:01:00Z"); }
            @Override public String getReportTitle() { return "망원시장 정책 보고서"; }
            @Override public String getStorageKey() { return storageKey; }
            @Override public String getOwnerName() { return ownerName; }
            @Override public Integer getPredictedRiskScore() { return riskScore; }
        };
    }

    /** 전체 조회가 어떤 인자로 불려도 주어진 행 하나를 돌려주도록 한다. */
    private void givenAllScenariosReturn(ReportQueryRepository.ReportRow row) {
        given(reportQueryRepository.findScenarioHistory(
                any(), any(), any(), anyBoolean(), any(), any(), any(Pageable.class)))
                .willAnswer(invocation -> new PageImpl<>(
                        List.of(row), invocation.getArgument(6), 1));
    }

    @Test
    @DisplayName("관제요원은 전체 이력을 조회할 수 없다")
    void rejectsNonAdmin() {
        givenCurrentUser("ROL02");

        assertThatThrownBy(() ->
                reportService.listAllScenarios(null, null, "all", false, null, null, 0, 10))
                .isInstanceOf(ForbiddenActionException.class)
                .hasMessageContaining("관리자만");
    }

    @Test
    @DisplayName("관리자는 전체 이력을 조회하고 실행자 이름과 위험 점수를 함께 받는다")
    void allowsAdminAndMapsOwnerAndRiskScore() {
        givenCurrentUser("ROL01");
        givenAllScenariosReturn(row("reports/a.docx", "관제요원", 82));

        var result = reportService.listAllScenarios(null, null, "all", false, null, null, 0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).ownerName()).isEqualTo("관제요원");
        assertThat(result.getContent().get(0).predictedRiskScore()).isEqualTo(82);
        assertThat(result.getContent().get(0).hasReport()).isTrue();
        assertThat(result.getContent().get(0).downloadPath())
                .isEqualTo("/api/simulation/reports/48/download");
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("보고서가 없는 실행은 제목과 다운로드 경로가 비어 나온다")
    void marksRunWithoutReport() {
        givenCurrentUser("ROL01");
        // 보고서를 만들기 전 실행은 generated_report_path가 NULL이다. 빈 문자열도 같이 다룬다.
        givenAllScenariosReturn(row("   ", null, null));

        var result = reportService.listAllScenarios(null, null, "all", false, null, null, 0, 10);

        assertThat(result.getContent().get(0).hasReport()).isFalse();
        assertThat(result.getContent().get(0).reportTitle()).isNull();
        assertThat(result.getContent().get(0).downloadPath()).isNull();
        assertThat(result.getContent().get(0).ownerName()).isNull();
    }

    @Test
    @DisplayName("위험 점수가 없는 옛 이력도 점수 필드가 null로 그대로 나온다")
    void keepsNullRiskScore() {
        givenCurrentUser("ROL01");
        // 위험도 계산 이전 데이터. 목록에서 사라지지 않고 null로 내려가야 한다
        // (화면이 "-"로 표시한다).
        givenAllScenariosReturn(row("reports/a.docx", "관제요원", null));

        var result = reportService.listAllScenarios(null, null, "all", false, null, null, 0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).predictedRiskScore()).isNull();
    }

    @Test
    @DisplayName("위험 점수 범위를 그대로 리포지토리에 넘긴다")
    void passesRiskScoreRangeThrough() {
        givenCurrentUser("ROL01");
        givenAllScenariosReturn(row("reports/a.docx", "관제요원", 90));

        reportService.listAllScenarios(1L, "화재", "all", true, 75, 100, 0, 10);

        // 범위를 서비스에서 임의로 보정하지 않는다 - 등급 경계는 화면이 정한다.
        verify(reportQueryRepository).findScenarioHistory(
                eq(1L), eq("화재"), eq("all"), eq(true), eq(75), eq(100), any(Pageable.class));
    }

    @Test
    @DisplayName("알 수 없는 검색 대상은 전체 검색으로 되돌린다")
    void fallsBackToAllSearchField() {
        givenCurrentUser("ROL01");
        givenAllScenariosReturn(row("reports/a.docx", "관제요원", 10));

        // 오타나 옛 클라이언트가 보낸 값이 그대로 내려가면 어떤 분기에도 걸리지 않아
        // "검색어를 넣었는데 항상 0건"이 된다.
        reportService.listAllScenarios(null, "화재", "scenarioName", false, null, null, 0, 10);

        verify(reportQueryRepository).findScenarioHistory(
                isNull(), eq("화재"), eq("all"), eq(false), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("검색 대상이 null이면 전체 검색으로 다룬다")
    void treatsNullSearchFieldAsAll() {
        givenCurrentUser("ROL01");
        givenAllScenariosReturn(row("reports/a.docx", "관제요원", 10));

        reportService.listAllScenarios(null, "화재", null, false, null, null, 0, 10);

        verify(reportQueryRepository).findScenarioHistory(
                isNull(), eq("화재"), eq("all"), eq(false), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("허용된 검색 대상은 그대로 넘긴다")
    void passesKnownSearchField() {
        givenCurrentUser("ROL01");
        givenAllScenariosReturn(row("reports/a.docx", "관제요원", 10));

        reportService.listAllScenarios(null, "화재", "policy", false, null, null, 0, 10);

        verify(reportQueryRepository).findScenarioHistory(
                isNull(), eq("화재"), eq("policy"), eq(false), isNull(), isNull(),
                any(Pageable.class));
    }

    @Test
    @DisplayName("빈 키워드는 null로 정규화해 검색 조건에서 빠진다")
    void normalizesBlankKeyword() {
        givenCurrentUser("ROL01");
        givenAllScenariosReturn(row("reports/a.docx", "관제요원", 10));

        reportService.listAllScenarios(null, "   ", "all", false, null, null, 0, 10);

        verify(reportQueryRepository).findScenarioHistory(
                isNull(), isNull(), eq("all"), eq(false), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("size가 상한을 넘으면 100으로 잘리고 page 음수는 0이 된다")
    void clampsPageable() {
        givenCurrentUser("ROL01");
        givenAllScenariosReturn(row("reports/a.docx", "관제요원", 10));

        reportService.listAllScenarios(null, null, "all", false, null, null, -5, 5000);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(reportQueryRepository).findScenarioHistory(
                any(), any(), any(), anyBoolean(), any(), any(), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("본인 이력 조회는 로그인 사용자 번호로 좁힌다")
    void myScenariosScopedToCurrentUser() {
        givenCurrentUser("ROL02");
        given(reportQueryRepository.findScenarioHistoryByUserId(
                anyLong(), any(), any(), anyBoolean(), any(), any(), any(Pageable.class)))
                .willAnswer(invocation -> new PageImpl<>(
                        List.of(row("reports/a.docx", "관제요원", 30)),
                        invocation.getArgument(6), 1));

        var result = reportService.listMyScenarios(9L, "망원", "all", false, 25, 49, 0, 10);

        assertThat(result.getContent()).hasSize(1);
        verify(reportQueryRepository).findScenarioHistoryByUserId(
                eq(9L), eq("망원"), eq("all"), eq(false), eq(25), eq(49), any(Pageable.class));
    }

    @Test
    @DisplayName("관제요원도 본인 이력은 조회할 수 있다(관리자 검증 대상이 아니다)")
    void myScenariosAllowedForNonAdmin() {
        givenCurrentUser("ROL02");
        given(reportQueryRepository.findScenarioHistoryByUserId(
                anyLong(), any(), any(), anyBoolean(), any(), any(), any(Pageable.class)))
                .willAnswer(invocation -> new PageImpl<>(
                        List.of(), invocation.getArgument(6), 0));

        // assertAdmin이 /my에 잘못 걸리면 관제요원이 자기 이력도 못 본다.
        var result = reportService.listMyScenarios(9L, null, "all", false, null, null, 0, 10);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("검색 키워드는 앞뒤 공백을 잘라 넘긴다")
    void trimsKeyword() {
        givenCurrentUser("ROL01");
        givenAllScenariosReturn(row("reports/a.docx", "관제요원", 10));

        reportService.listAllScenarios(null, "  망원시장  ", "all", false, null, null, 0, 10);

        verify(reportQueryRepository).findScenarioHistory(
                isNull(), eq("망원시장"), eq("all"), eq(false), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("size가 0이면 기본값 10을 쓴다(게시판과 동일)")
    void fallsBackToDefaultSize() {
        givenCurrentUser("ROL01");
        givenAllScenariosReturn(row("reports/a.docx", "관제요원", 10));

        reportService.listAllScenarios(null, null, "all", false, null, null, 0, 0);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(reportQueryRepository).findScenarioHistory(
                any(), any(), any(), anyBoolean(), any(), any(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(10);
    }
}
