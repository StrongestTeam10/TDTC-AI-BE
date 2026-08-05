package com.markettwin.backend.dto.response;

import java.math.BigDecimal;

/**
 * 2026-08-XX 추가: 지도에 건물 폴리곤을 그리기 위한 DTO. mrkbldg01m을 그대로 내려준다.
 * polygonCoordinates는 ZoneDto와 동일하게 GeoJSON [경도,위도] 문자열이라,
 * 프론트에서 Zone과 같은 파싱 함수를 그대로 재사용할 수 있다.
 */
public record BuildingDto(
        Long buildingId,
        Long marketId,
        String pnuCode,
        String polygonCoordinates,
        BigDecimal heightM,
        Boolean heightEstimated,
        Integer floors
) {
}