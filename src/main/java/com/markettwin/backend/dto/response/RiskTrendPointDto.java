package com.markettwin.backend.dto.response;

import java.util.List;

/**
 * 예측 시뮬레이션의 특정 스텝 시점 위험도 추이 데이터. SIM RiskTrendPoint와 1:1 매칭.
 */
public record RiskTrendPointDto(
        Integer step,
        Double overallRiskScore,
        List<ZoneRiskPointDto> zones
) {
}
