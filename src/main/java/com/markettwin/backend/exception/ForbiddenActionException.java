package com.markettwin.backend.exception;

// 게시판 기능
// 본인 글이 아닌데 수정/삭제를 시도하거나, 관리자가 아닌데 공지 고정을 시도하는 경우 등
// "인증은 됐지만 권한이 없는" 상황에 던짐 (401이 아니라 403에 대응)
public class ForbiddenActionException extends RuntimeException {
    public ForbiddenActionException(String message) {
        super(message);
    }
}
