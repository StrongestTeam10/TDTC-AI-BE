package com.markettwin.backend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AI 파이프라인 웹훅(X-API-KEY)의 인증 판정을 확인한다.
 *
 * 이 판정이 틀리면 인증 자체가 뚫린다. 특히 아래 두 경우는 실제로 있었던 결함이라
 * 회귀하지 않도록 테스트로 고정한다.
 *
 * 1) 설정 키가 비어 있는데 요청도 빈 값이면 통과하던 문제.
 *    ai.secret-key는 `${AI_SECRET_KEY:}`라 환경변수가 없으면 빈 문자열이 된다.
 *    예전 코드(`!apiKey.equals(aiSecretKey)`)에서는 `"".equals("")`가 참이라
 *    키를 설정하지 않은 서버의 웹훅이 무인증으로 열려 있었다.
 * 2) String.equals의 조기 반환. 상수시간 비교로 바뀌었는지는 타이밍을 직접
 *    재는 대신, 판정 결과가 예전과 같은지(같은 값 참 / 다른 값 거짓)로 확인한다.
 */
class ApiKeysTest {

    private static final String CONFIGURED = "tdtc-ai-webhook-secret-1234567890";

    @Test
    @DisplayName("설정된 키와 정확히 같으면 통과한다")
    void matchingKeyPasses() {
        assertThat(ApiKeys.matches(CONFIGURED, CONFIGURED)).isTrue();
    }

    @Test
    @DisplayName("키가 다르면 거부한다")
    void differentKeyIsRejected() {
        assertThat(ApiKeys.matches("wrong-key", CONFIGURED)).isFalse();
    }

    @Test
    @DisplayName("앞부분만 같은 키는 거부한다")
    void sharedPrefixIsRejected() {
        assertThat(ApiKeys.matches("tdtc-ai-webhook-secret-000", CONFIGURED)).isFalse();
    }

    @Test
    @DisplayName("설정 키의 앞부분을 잘라 보낸 키는 거부한다 - 길이가 달라도 통과하면 안 된다")
    void truncatedKeyIsRejected() {
        assertThat(ApiKeys.matches(CONFIGURED.substring(0, 10), CONFIGURED)).isFalse();
    }

    @Test
    @DisplayName("설정 키 뒤에 문자를 덧붙인 키는 거부한다")
    void extendedKeyIsRejected() {
        assertThat(ApiKeys.matches(CONFIGURED + "x", CONFIGURED)).isFalse();
    }

    @Test
    @DisplayName("헤더가 없으면(null) 거부한다")
    void missingHeaderIsRejected() {
        assertThat(ApiKeys.matches(null, CONFIGURED)).isFalse();
    }

    @Test
    @DisplayName("설정 키가 비어 있으면 빈 헤더를 보내도 거부한다 - 예전엔 여기서 뚫렸다")
    void emptyConfiguredKeyRejectsEmptyHeader() {
        assertThat(ApiKeys.matches("", "")).isFalse();
    }

    @Test
    @DisplayName("설정 키가 비어 있으면 어떤 값을 보내도 거부한다")
    void emptyConfiguredKeyRejectsAnything() {
        assertThat(ApiKeys.matches("아무값", "")).isFalse();
        assertThat(ApiKeys.matches(null, "")).isFalse();
    }

    @Test
    @DisplayName("설정 키가 null이어도 거부한다")
    void nullConfiguredKeyIsRejected() {
        assertThat(ApiKeys.matches("아무값", null)).isFalse();
        assertThat(ApiKeys.matches(null, null)).isFalse();
    }

    @Test
    @DisplayName("설정 키가 비어 있지 않으면 빈 헤더는 거부한다")
    void emptyHeaderIsRejectedWhenKeyConfigured() {
        assertThat(ApiKeys.matches("", CONFIGURED)).isFalse();
    }

    @Test
    @DisplayName("대소문자가 다르면 거부한다")
    void caseSensitiveComparison() {
        assertThat(ApiKeys.matches(CONFIGURED.toUpperCase(), CONFIGURED)).isFalse();
    }

    @Test
    @DisplayName("앞뒤 공백이 붙으면 거부한다 - 헤더 값을 임의로 다듬지 않는다")
    void whitespaceIsNotTrimmed() {
        assertThat(ApiKeys.matches(" " + CONFIGURED, CONFIGURED)).isFalse();
        assertThat(ApiKeys.matches(CONFIGURED + " ", CONFIGURED)).isFalse();
    }

    @Test
    @DisplayName("멀티바이트 키도 정확히 비교한다 - UTF-8 바이트로 변환해도 판정이 흔들리지 않는다")
    void multibyteKeyIsComparedExactly() {
        String korean = "비밀키-한글-1234";
        assertThat(ApiKeys.matches(korean, korean)).isTrue();
        assertThat(ApiKeys.matches("비밀키-한글-1235", korean)).isFalse();
    }
}
