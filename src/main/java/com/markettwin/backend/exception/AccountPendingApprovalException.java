package com.markettwin.backend.exception;

// 회원가입 관리자 승인
public class AccountPendingApprovalException extends RuntimeException {
    public AccountPendingApprovalException() {
        super("관리자 승인 대기 중인 계정입니다. 승인 후 로그인할 수 있습니다.");
    }
}
