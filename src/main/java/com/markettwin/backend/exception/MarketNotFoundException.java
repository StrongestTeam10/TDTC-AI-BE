package com.markettwin.backend.exception;

// 시장/구역별 권한 분리
public class MarketNotFoundException extends RuntimeException {
    public MarketNotFoundException(Long marketId) {
        super("시장을 찾을 수 없습니다: " + marketId);
    }
}
