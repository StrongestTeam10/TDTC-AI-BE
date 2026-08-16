package com.markettwin.backend.service;

import com.markettwin.backend.client.SimulationEngineClient;
import com.markettwin.backend.domain.entity.User;
import com.markettwin.backend.dto.request.PredictRequestDto;
import com.markettwin.backend.dto.request.ScenarioRequestDto;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 시뮬레이션 실행이 요청받은 시장의 접근 권한을 먼저 확인하는지 본다.
 *
 * 검증이 없던 동안에는 관제요원이 요청 본문의 marketId만 바꿔 담당이 아닌 시장의
 * 시뮬레이션을 실행할 수 있었다. 거부됐을 때 SIM 호출과 DB 적재까지 함께 막히는지가
 * 요점이라, 순서를 확인할 수 있는 단위 테스트로 둔다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SimulationServiceMarketScopeTest {

    private static final Long OTHER_MARKET_ID = 2L;

    @Mock
    private SimulationEngineClient simulationEngineClient;

    @Mock
    private ScenarioRepository scenarioRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private MarketService marketService;

    @Mock
    private ObservedPlacementService observedPlacementService;

    @InjectMocks
    private SimulationService simulationService;

    private void givenMarketOutOfScope() {
        User currentUser = User.builder()
                .userId(5L).loginId("city").rulesCode("ROL02").marketCode("MKTMW").build();
        given(currentUserProvider.getCurrentUser()).willReturn(currentUser);
        willThrow(new ForbiddenActionException("담당 시장이 아니라 조회할 수 없습니다: " + OTHER_MARKET_ID))
                .given(marketService).getAccessibleMarket(OTHER_MARKET_ID, currentUser);
    }

    private ScenarioRequestDto scenarioRequest() {
        return new ScenarioRequestDto(
                OTHER_MARKET_ID, 100, 30, List.of(), List.of(), List.of(), List.of(), null, List.of());
    }

    @Test
    @DisplayName("담당이 아닌 시장의 시나리오 실행은 거부된다")
    void rejectsScenarioOnOtherMarket() {
        givenMarketOutOfScope();

        assertThatThrownBy(() -> simulationService.runScenario(scenarioRequest()))
                .isInstanceOf(ForbiddenActionException.class)
                .hasMessageContaining("담당 시장이 아니라");
    }

    @Test
    @DisplayName("거부되면 SIM 호출과 시나리오 저장이 모두 일어나지 않는다")
    void doesNotRunOrPersistWhenRejected() {
        givenMarketOutOfScope();

        assertThatThrownBy(() -> simulationService.runScenario(scenarioRequest()))
                .isInstanceOf(ForbiddenActionException.class);

        // 검증이 저장 뒤에 있으면 남의 시장 이력이 simscnr01m에 남는다.
        verify(scenarioRepository, never()).save(any());
        verify(simulationEngineClient, never()).runScenario(any());
    }

    @Test
    @DisplayName("담당이 아닌 시장의 예측 실행도 거부된다")
    void rejectsPredictOnOtherMarket() {
        givenMarketOutOfScope();

        assertThatThrownBy(() -> simulationService.predict(
                new PredictRequestDto(OTHER_MARKET_ID, null, 30, 100,
                        List.of(), List.of(), List.of(), List.of(), List.of(), null)))
                .isInstanceOf(ForbiddenActionException.class);

        verify(simulationEngineClient, never()).predict(any());
    }
}
