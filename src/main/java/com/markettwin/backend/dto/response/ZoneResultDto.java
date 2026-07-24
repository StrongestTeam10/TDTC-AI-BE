package com.markettwin.backend.dto.response;

/**
 * 파이프라인 A 스냅샷의 구역별 위험도 결과. SIM ZoneResult와 1:1 매칭.
 */
public record ZoneResultDto(
        Long zoneId,
        String zoneName,
        Double areaM2,
        Double pathWidthM,
        Integer visitorCount,
        Double density,
        Double personalSpace,
        Double riskScore,
        String riskLevel,
        String reason,
        RiskBreakdownDto breakdown
) {
}
