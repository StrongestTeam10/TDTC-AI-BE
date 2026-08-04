package com.markettwin.backend.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 2026-08-04 추가 (비밀번호 찾기)
 *
 * 일부러 예외(404 등)를 던지지 않고 200 OK + verified:false로 응답한다. 아이디
 * 존재 여부 자체를 HTTP 상태코드로 노출하지 않기 위함(로그인 실패를
 * InvalidCredentialsException 하나로 통일한 것과 같은 이유).
 */
@Getter
@Builder
public class VerifyIdentityResponseDto {
    private boolean verified;
}
