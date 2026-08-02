package com.markettwin.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * SIM ZoneInfo와 1:1. 시장 구역(mrkaddr01d) 한 행.
 *
 * 보고서 문장에 "1구역" 대신 "남측 구역"처럼 실제 이름을 쓰기 위해 보낸다.
 * SIM은 보고서 생성 시 DB를 조회하지 않으므로 번들에 함께 실어야 한다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ZoneInfoDto(
        Long zoneId,
        String zoneName
) {
}
