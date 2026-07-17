package com.markettwin.backend.dto.response;

import java.time.Instant;
import java.util.List;

public record DashboardSnapshotDto(
        Instant snapshotTime,
        List<AgentStateDto> agents,
        RiskScoreDto riskScore,
        List<AlertLogDto> alerts
) {
}
