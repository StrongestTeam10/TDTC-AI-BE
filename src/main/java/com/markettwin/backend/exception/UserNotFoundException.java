package com.markettwin.backend.exception;

// 2026-08-05 추가 (회원관리 - 관리자 권한 변경 대상 사용자를 찾을 수 없을 때)
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long userId) {
        super("사용자를 찾을 수 없습니다: " + userId);
    }
}
