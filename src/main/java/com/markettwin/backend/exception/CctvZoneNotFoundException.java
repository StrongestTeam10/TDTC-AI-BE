package com.markettwin.backend.exception;

// 2026-08-11 추가 (CCTV 관제 구역)
public class CctvZoneNotFoundException extends RuntimeException {
    public CctvZoneNotFoundException(Long cctvZoneId) {
        super("CCTV 구역을 찾을 수 없습니다: " + cctvZoneId);
    }
}
