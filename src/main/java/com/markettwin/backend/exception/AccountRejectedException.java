package com.markettwin.backend.exception;

// 2026-08-04 추가 (회원가입 관리자 승인)
public class AccountRejectedException extends RuntimeException {
    public AccountRejectedException() {
        super("가입이 거부된 계정입니다. 관리자에게 문의해주세요.");
    }
}
