package com.markettwin.backend.exception;

// 게시판 - 회원가입 시장 선택
public class InvalidMarketCodeException extends RuntimeException {
    public InvalidMarketCodeException(String marketCode) {
        super("유효하지 않은 담당 시장 코드입니다: " + marketCode);
    }
}
