package com.markettwin.backend.exception;

/**
 * 2026-08-14 추가 (시장 경계 제안): OpenStreetMap(Overpass) 연동이 실패했을 때.
 *
 * "OSM에 이 시장이 폴리곤으로 등록돼 있지 않다"는 것은 오류가 아니라 정상적인 결과이므로
 * 이 예외를 쓰지 않는다(MarketBoundaryDto.found=false로 돌려준다). 여기로 오는 것은
 * 호출 실패·응답 해석 실패·Overpass 호출량 제한처럼 사용자가 어쩔 수 없는 경우뿐이다.
 */
public class MarketBoundaryException extends RuntimeException {
    public MarketBoundaryException(String message) {
        super(message);
    }

    public MarketBoundaryException(String message, Throwable cause) {
        super(message, cause);
    }
}
