package com.markettwin.backend.dto.response;

import java.math.BigDecimal;

public record MarketDto(
        Long marketId,
        String marketName,
        BigDecimal latitude,
        BigDecimal longitude,
        // 2026-08-14 추가 (시장 등록): comcode01m MKT 도메인 코드. 등록 직후 화면이
        // 어떤 코드로 만들어졌는지 보여주고, 담당 시장 판정이 이 값 기준임을 드러낸다.
        String marketCode
) {
}
