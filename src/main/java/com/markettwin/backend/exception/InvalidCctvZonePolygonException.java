package com.markettwin.backend.exception;

// CCTV 관제 구역
// CCTV 호모그래피 ROI는 사각형 4점이어야 해서, 형식이 어긋나면 저장 전에 막는다.
public class InvalidCctvZonePolygonException extends RuntimeException {
    public InvalidCctvZonePolygonException(String reason) {
        super("CCTV 구역 좌표가 올바르지 않습니다: " + reason);
    }
}
