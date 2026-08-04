package com.markettwin.backend.dto.response;

import java.time.Instant;

public record RiskDto(
        Long riskId,
        Long coordId,
        Float riskScore,
        String riskLevel,
        String reasonCode,
        Instant detectedAt,
        Integer totalCount, // 👈 추가
        String videoUrl
) {
}
