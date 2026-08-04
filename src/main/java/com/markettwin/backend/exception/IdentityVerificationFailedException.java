package com.markettwin.backend.exception;

// 2026-08-04 추가 (비밀번호 찾기)
// reset-password 시점에 아이디+이름+소속기관+담당시장 재검증이 실패한 경우.
// verify-identity 단계는 예외 없이 verified:false로 응답하지만(VerifyIdentityResponseDto
// 참고), reset-password는 실제로 비밀번호를 바꾸는 동작이라 실패를 명확히 오류로 알림.
public class IdentityVerificationFailedException extends RuntimeException {
    public IdentityVerificationFailedException() {
        super("입력하신 정보와 일치하는 계정을 찾을 수 없습니다.");
    }
}
