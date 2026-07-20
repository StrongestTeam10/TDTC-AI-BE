package com.markettwin.backend.dto.response;

import java.math.BigDecimal;

public record MarketDto(
        Long marketId,
        String marketName,
        BigDecimal latitude,
        BigDecimal longitude
) {
}
