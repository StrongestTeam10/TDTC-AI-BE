package com.markettwin.backend.dto.response;

public record ZoneDto(
        Long zoneId,
        Long marketId,
        String zoneName,
        String polygonCoordinates   // GeoJSON 또는 WKT 문자열 (프론트에서 파싱)
) {
}
