package com.markettwin.backend.exception;

public class InvalidOrgCodeException extends RuntimeException {
    public InvalidOrgCodeException(String orgCode) {
        super("유효하지 않은 소속기관 코드입니다: " + orgCode);
    }
}
