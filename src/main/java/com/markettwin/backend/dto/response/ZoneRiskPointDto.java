package com.markettwin.backend.dto.response;

/**
 * 예측 결과의 스텝별 구역 위험도 (그래프용, ZoneResultDto보다 가벼운 요약). SIM
 * ZoneRiskPoint와 1:1 매칭.
 */
public record ZoneRiskPointDto(
        Long zoneId,
        Double riskScore,
        String riskLevel
) {
}
