package com.markettwin.backend.dto.response;

/**
 * 2026-07-30 추가 (보고서 기능)
 * 이미 생성된 보고서의 다운로드 주소.
 *
 * 302 리다이렉트가 아니라 JSON으로 URL을 돌려주는 이유:
 *   - 이 API는 JWT 인증이 필요한데, 브라우저가 단순 이동하면 Authorization 헤더가 실리지 않는다.
 *   - fetch로 부르면 헤더는 실리지만 리다이렉트를 따라 S3로 갈 때 CORS에 막힌다.
 * URL만 받아 클라이언트가 직접 이동하면 두 문제가 모두 사라지고 S3 CORS 설정도 필요 없다.
 */
public record ReportDownloadDto(
        Long scenarioId,

        // 만료 시간이 있는 presigned URL. 만료되면 이 API를 다시 호출하면 된다.
        String downloadUrl,

        // URL 유효 시간(초). 클라이언트가 만료 시점을 판단할 수 있게 함께 준다.
        long expiresInSeconds
) {
}
