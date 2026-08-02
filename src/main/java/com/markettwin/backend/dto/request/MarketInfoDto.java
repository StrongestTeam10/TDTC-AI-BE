package com.markettwin.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;

/**
 * SIM MarketInfo와 1:1. 보고서 대상 시장 정보(mrkaddr01m).
 *
 * 위경도는 좌표가 없는 시장도 있어 null을 허용한다. SIM에서도 선택 항목이라
 * 없으면 해당 표기만 빠지고 보고서는 정상 생성된다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record MarketInfoDto(
        Long marketId,
        String marketName,
        BigDecimal latitude,
        BigDecimal longitude
) {
}
