package com.markettwin.backend.exception;

// 2026-07-24 추가
// 아이디가 없는 경우/비밀번호가 틀린 경우 둘 다 이 예외 하나로 통일해서 던짐
// (아이디 존재 여부를 노출하지 않기 위한 통상적인 보안 관례).
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("아이디 또는 비밀번호가 올바르지 않습니다.");
    }
}
