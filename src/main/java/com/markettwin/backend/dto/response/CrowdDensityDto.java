package com.markettwin.backend.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record CrowdDensityDto(
        Long crowdDensityId,
        Long zoneId,
        Integer visitorCount,
        BigDecimal densityScore,
        String statusLevel,
        Instant capturedAt
) {
}
