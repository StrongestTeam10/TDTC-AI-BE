package com.markettwin.backend.exception;

// 2026-07-24 추가
public class DuplicateLoginIdException extends RuntimeException {
    public DuplicateLoginIdException(String loginId) {
        super("이미 사용 중인 아이디입니다: " + loginId);
    }
}
