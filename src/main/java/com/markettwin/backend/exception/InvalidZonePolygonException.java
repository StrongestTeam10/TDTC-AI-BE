package com.markettwin.backend.exception;

/**
 * 2026-08-14 추가 (구역 등록): 구역 폴리곤이 GeoJSON Polygon 형식이 아니거나
 * 꼭짓점이 모자라거나 좌표 범위를 벗어났을 때.
 *
 * CCTV 구역용 InvalidCctvZonePolygonException과 나눠 둔 이유: CCTV는 사각형 4점
 * 고정에 소속 구역 안에 있어야 한다는 제약이 더 붙어서, 같은 예외로 묶으면
 * 사용자에게 어느 쪽 규칙을 어겼는지 안 보인다.
 */
public class InvalidZonePolygonException extends RuntimeException {
    public InvalidZonePolygonException(String message) {
        super(message);
    }
}
