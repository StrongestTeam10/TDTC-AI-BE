package com.markettwin.backend.dto.response;

/**
 * 2026-07-25 추가: 지도에 통로(구역 간 연결)를 선으로 그리고 클릭으로 정책을
 * 지정할 수 있도록, mrkadjc01m 원본을 그대로 프론트에 내려주는 DTO.
 */
public record ZoneAdjacencyDto(
        Long adjacencyId,
        Long fromZoneId,
        Long toZoneId,
        String pathCoordinates,  // GeoJSON LineString 문자열, null이면 프론트가 두 구역 중심을 이어서 대체
        Boolean isActive
) {
}