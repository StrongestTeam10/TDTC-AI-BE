package com.markettwin.backend.exception;

// (회원관리) - AuthService의 InvalidOrgCodeException/InvalidMarketCodeException과
// 동일한 패턴. comcode01m(code_cob='ROL')에 존재하지 않는 권한 코드로 변경 시도할 때 던짐.
public class InvalidRoleCodeException extends RuntimeException {
    public InvalidRoleCodeException(String rulesCode) {
        super("유효하지 않은 권한 코드입니다: " + rulesCode);
    }
}
