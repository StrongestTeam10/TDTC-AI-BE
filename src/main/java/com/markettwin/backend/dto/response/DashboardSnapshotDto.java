package com.markettwin.backend.dto.response;

import java.time.Instant;
import java.util.List;

public record DashboardSnapshotDto(
        Instant snapshotTime,
        List<CrowdDensityDto> crowdDensities,
        List<RiskDto> risks,
        List<AgentStateDto> agents   // Mesa 시뮬레이션 엔진(FastAPI)에서 받아오는 값 - DB 테이블 아님
) {
}
