package com.markettwin.backend.service;

import com.markettwin.backend.domain.entity.Scenario;
import com.markettwin.backend.domain.entity.User;
import com.markettwin.backend.exception.ForbiddenActionException;
import com.markettwin.backend.repository.ScenarioRepository;
import com.markettwin.backend.security.CurrentUserProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * 보고서 API의 소유자 검증을 확인한다.
 *
 * 이 검증이 없던 동안에는 인증만 통과하면 남의 scenarioId로 보고서를 생성하거나
 * 내려받을 수 있었다. 거부 경로는 관리자 계정으로는 재현되지 않으므로(관리자는
 * 우회 대상) 단위 테스트로 확인한다.
 *
 * 진입점은 reissueDownloadUrl을 쓴다. generate와 같은 assertOwner를 타면서도
 * 거부 시 SIM 호출이나 S3 접근 이전에 끝나 준비할 의존성이 적다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReportServiceOwnerCheckTest {

    private static final Long SCENARIO_ID = 48L;

    @Mock
    private ScenarioRepository scenarioRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private ReportService reportService;

    private void givenScenarioOwnedBy(Long ownerUserId) {
        given(scenarioRepository.findById(SCENARIO_ID)).willReturn(
                Optional.of(Scenario.builder()
                        .scenarioId(SCENARIO_ID)
                        .userId(ownerUserId)
                        .marketId(1L)
                        .build()));
    }

    private void givenCurrentUser(Long userId, String rulesCode) {
        given(currentUserProvider.getCurrentUser()).willReturn(
                User.builder()
                        .userId(userId)
                        .loginId("user" + userId)
                        .rulesCode(rulesCode)
                        .build());
    }

    @Test
    @DisplayName("다른 사용자가 실행한 시나리오는 보고서를 내려받을 수 없다")
    void rejectsOtherUsersScenario() {
        givenScenarioOwnedBy(1L);
        givenCurrentUser(2L, "ROL03");

        assertThatThrownBy(() -> reportService.reissueDownloadUrl(SCENARIO_ID))
                .isInstanceOf(ForbiddenActionException.class)
                .hasMessageContaining("본인이 실행한");
    }

    @Test
    @DisplayName("user_id가 비어 있는 옛 시나리오는 소유자를 알 수 없으므로 거부한다")
    void rejectsScenarioWithoutOwner() {
        givenScenarioOwnedBy(null);
        givenCurrentUser(2L, "ROL03");

        assertThatThrownBy(() -> reportService.reissueDownloadUrl(SCENARIO_ID))
                .isInstanceOf(ForbiddenActionException.class)
                .hasMessageContaining("실행자 정보가 없는");
    }

    @Test
    @DisplayName("관리자는 남의 시나리오도 다룰 수 있다")
    void allowsAdminOnOtherUsersScenario() {
        givenScenarioOwnedBy(1L);
        givenCurrentUser(2L, "ROL01");

        // 소유자 검증을 통과하면 그다음 단계(결과 조회)로 넘어가므로 여기서 막히지 않는다.
        // 뒤에서 어떤 예외가 나든 ForbiddenActionException만 아니면 통과한 것이다.
        assertThatThrownBy(() -> reportService.reissueDownloadUrl(SCENARIO_ID))
                .isNotInstanceOf(ForbiddenActionException.class);
    }

    @Test
    @DisplayName("본인이 실행한 시나리오는 소유자 검증에서 막히지 않는다")
    void allowsOwner() {
        givenScenarioOwnedBy(7L);
        givenCurrentUser(7L, "ROL03");

        assertThatThrownBy(() -> reportService.reissueDownloadUrl(SCENARIO_ID))
                .isNotInstanceOf(ForbiddenActionException.class);
    }
}
