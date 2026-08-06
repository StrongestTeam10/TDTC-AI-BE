package com.markettwin.backend.service;

import com.markettwin.backend.domain.entity.User;
import com.markettwin.backend.dto.response.ScenarioHistoryDto;
import com.markettwin.backend.exception.ForbiddenActionException;
import com.markettwin.backend.repository.ReportQueryRepository;
import com.markettwin.backend.security.CurrentUserProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;

/**
 * 전체 시뮬레이션 이력 조회(listAllScenarios)의 권한과 매핑을 확인한다.
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

    /** 네이티브 쿼리 프로젝션 대역. 보고서가 있는 실행 한 줄을 흉내낸다. */
    private ReportQueryRepository.ReportRow row(String storageKey, String ownerName) {
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
        };
    }

    @Test
    @DisplayName("관제요원은 전체 이력을 조회할 수 없다")
    void rejectsNonAdmin() {
        givenCurrentUser("ROL02");

        assertThatThrownBy(() -> reportService.listAllScenarios(null))
                .isInstanceOf(ForbiddenActionException.class)
                .hasMessageContaining("관리자만");
    }

    @Test
    @DisplayName("관리자는 전체 이력을 조회하고 실행자 이름을 함께 받는다")
    void allowsAdminAndMapsOwnerName() {
        givenCurrentUser("ROL01");
        given(reportQueryRepository.findScenarioHistory(isNull()))
                .willReturn(List.of(row("reports/a.docx", "관제요원")));

        List<ScenarioHistoryDto> result = reportService.listAllScenarios(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).ownerName()).isEqualTo("관제요원");
        assertThat(result.get(0).hasReport()).isTrue();
        assertThat(result.get(0).downloadPath())
                .isEqualTo("/api/simulation/reports/48/download");
    }

    @Test
    @DisplayName("보고서가 없는 실행은 제목과 다운로드 경로가 비어 나온다")
    void marksRunWithoutReport() {
        givenCurrentUser("ROL01");
        // 보고서를 만들기 전 실행은 generated_report_path가 NULL이다. 빈 문자열도 같이 다룬다.
        given(reportQueryRepository.findScenarioHistory(isNull()))
                .willReturn(List.of(row("   ", null)));

        List<ScenarioHistoryDto> result = reportService.listAllScenarios(null);

        assertThat(result.get(0).hasReport()).isFalse();
        assertThat(result.get(0).reportTitle()).isNull();
        assertThat(result.get(0).downloadPath()).isNull();
        assertThat(result.get(0).ownerName()).isNull();
    }
}
