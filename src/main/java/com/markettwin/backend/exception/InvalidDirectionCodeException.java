package com.markettwin.backend.exception;

// 상점 외관 직접 촬영 데이터 수집 파이프라인
// comcode01m DIR 도메인(DIRNO/DIREA/DIRSO/DIRWE)에 없는 값이 들어온 경우
public class InvalidDirectionCodeException extends RuntimeException {
    public InvalidDirectionCodeException(String directionCode) {
        super("유효하지 않은 방향 코드입니다: " + directionCode);
    }
}
