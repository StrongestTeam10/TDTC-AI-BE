package com.markettwin.backend.exception;

// 2026-07-24 추가 (게시판 카테고리 기능)
public class InvalidCategoryCodeException extends RuntimeException {
    public InvalidCategoryCodeException(String categoryCode) {
        super("유효하지 않은 게시판 카테고리 코드입니다: " + categoryCode);
    }
}
