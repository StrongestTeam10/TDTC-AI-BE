package com.markettwin.backend.exception;

/**
 * 2026-08-14 추가 (시장 등록): 시장 코드 또는 시장 이름이 이미 쓰이고 있을 때.
 *
 * 이름까지 막는 이유는 comcode01m.code_name에 테이블 전체 UNIQUE 제약이 걸려
 * 있기 때문이다(V1__baseline_schema.sql). 미리 확인하지 않으면
 * DataIntegrityViolationException이 그대로 올라가 500으로 새어 나가고,
 * 사용자는 "왜 실패했는지" 알 수 없다.
 */
public class DuplicateMarketException extends RuntimeException {
    public DuplicateMarketException(String message) {
        super(message);
    }
}
