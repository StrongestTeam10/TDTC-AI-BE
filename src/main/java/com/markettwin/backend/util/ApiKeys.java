package com.markettwin.backend.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * AI 파이프라인 웹훅의 X-API-KEY 검증.
 *
 * 지금까지는 컨트롤러마다 `apiKey == null || !apiKey.equals(aiSecretKey)`를 따로
 * 적고 있었다(AiIntegrationController, PostReportController, VideoClipController).
 * 같은 검사를 여러 번 적으면서 두 가지 문제가 있었다.
 *
 * 1) String.equals는 첫 글자가 다르면 바로 끝난다. 응답 시간이 "앞에서 몇 글자가
 *    맞았는지"에 따라 달라지므로, 키를 한 글자씩 넓혀가며 맞히는 타이밍 공격이
 *    이론적으로 가능하다. MessageDigest.isEqual은 길이가 같으면 전체를 끝까지
 *    비교해서 이 신호를 없앤다(상수시간 비교).
 *
 * 2) ai.secret-key는 application.yml에서 `${AI_SECRET_KEY:}`라 환경변수가 없으면
 *    빈 문자열이 된다. 그 상태에서 예전 코드는 `X-API-KEY:` 를 빈 값으로 보내면
 *    "".equals("")가 true라 인증이 그냥 통과했다. 키가 설정되지 않은 서버는
 *    열려 있는 게 아니라 닫혀 있어야 하므로, 설정값이 비면 무조건 거부한다.
 */
public final class ApiKeys {

    private ApiKeys() {
    }

    /**
     * 제시된 키가 설정된 키와 일치하는지 상수시간으로 확인한다.
     *
     * @param presented 요청의 X-API-KEY 헤더 값 (헤더가 없으면 null)
     * @param configured 설정된 키(ai.secret-key). 비어 있으면 항상 false
     * @return 둘이 일치하고 설정 키가 비어 있지 않을 때만 true
     */
    public static boolean matches(String presented, String configured) {
        // 키가 설정되지 않은 서버는 이 엔드포인트를 열지 않는다(위 2번 참고).
        if (configured == null || configured.isEmpty()) {
            return false;
        }
        if (presented == null) {
            return false;
        }
        return MessageDigest.isEqual(
                presented.getBytes(StandardCharsets.UTF_8),
                configured.getBytes(StandardCharsets.UTF_8));
    }
}
