package com.markettwin.backend.exception;

// 2026-08-14 추가 (구역 등록/수정/삭제)
public class ZoneNotFoundException extends RuntimeException {
    public ZoneNotFoundException(Long zoneId) {
        super("구역을 찾을 수 없습니다: " + zoneId);
    }
}
