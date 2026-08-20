package com.markettwin.backend.service;

import com.markettwin.backend.domain.entity.CommonCode;
import com.markettwin.backend.repository.CommonCodeRepository;
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
import static org.mockito.BDDMockito.given;

/**
 * 표시용 시나리오명 조립을 확인한다.
 *
 * 이 이름은 목록과 보고서 문서(시나리오 구성표·결과 비교표) 양쪽에 그대로 실리므로,
 * 형식이 바뀌면 사용자에게 바로 보인다. 특히 "사람이 붙인 이름은 건드리지 않는다"는
 * 규칙이 깨지면 사용자가 입력한 이름이 조용히 사라진다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScenarioDisplayNameResolverTest {

    /** 09:12 KST. 저장은 UTC라 9시간 앞선 값을 넣는다. */
    private static final Instant REG_DATETIME = Instant.parse("2026-08-06T00:12:00Z");

    @Mock
    private CommonCodeRepository commonCodeRepository;

    @InjectMocks
    private ScenarioDisplayNameResolver resolver;

    private void givenPolicyCodes() {
        given(commonCodeRepository.findByCodeCob("POL")).willReturn(List.of(
                CommonCode.builder().codeCob("POL").code("POLFR").codeName("화재").build(),
                CommonCode.builder().codeCob("POL").code("POLCB").codeName("통로폐쇄").build()));
    }

    @Test
    @DisplayName("자동 생성된 이름은 날짜가 맨 앞에 오는 형태로 바뀐다")
    void putsDateFirst() {
        givenPolicyCodes();

        String name = resolver.resolve(
                "시나리오 2026-08-06T00:12:00.123456Z", "망원시장", "POLFR", REG_DATETIME);

        assertThat(name).isEqualTo("2026-08-06 09:12 망원시장 화재 시나리오");
    }

    @Test
    @DisplayName("사람이 붙인 이름은 그대로 둔다")
    void keepsUserProvidedName() {
        givenPolicyCodes();

        String name = resolver.resolve(
                "남측 통로 폐쇄 검토안", "망원시장", "POLCB", REG_DATETIME);

        assertThat(name).isEqualTo("남측 통로 폐쇄 검토안");
    }

    @Test
    @DisplayName("정책 유형이 '없음'이면 이름에 넣지 않는다")
    void omitsNeutralPolicy() {
        givenPolicyCodes();

        // POLNO를 넣으면 "망원시장 없음 시나리오"가 되어 오히려 읽기 나쁘다.
        String name = resolver.resolve(
                "시나리오 2026-08-06T00:12:00Z", "망원시장", "POLNO", REG_DATETIME);

        assertThat(name).isEqualTo("2026-08-06 09:12 망원시장 시나리오");
    }

    @Test
    @DisplayName("등록 시각이 없으면 날짜 없이 조립한다")
    void handlesMissingDate() {
        givenPolicyCodes();

        String name = resolver.resolve(
                "시나리오 2026-08-06T00:12:00Z", "망원시장", "POLFR", null);

        assertThat(name).isEqualTo("망원시장 화재 시나리오");
    }
}
