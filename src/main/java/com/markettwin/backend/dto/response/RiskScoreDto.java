package com.markettwin.backend.dto.response;

import java.time.Instant;

public record RiskScoreDto(
        Instant timestamp,
        Double score,           // 0 ~ 100
        String level,           // "low" | "medium" | "high" | "critical"
        ContributingFactors contributingFactors
) {
    public record ContributingFactors(
            Double density,
            Double acoustic,
            Double flowRate
    ) {
    }
}
