package com.markettwin.backend.dto.response;

import java.time.Instant;

public record RiskDto(
        Long riskId,
        Long marketId,
        Long zoneId,
        Float riskScore,
        String riskLevel,
        String reasonCode,
        Instant detectedAt
) {
}
