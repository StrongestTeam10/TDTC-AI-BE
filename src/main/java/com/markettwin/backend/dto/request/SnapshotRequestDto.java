package com.markettwin.backend.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * 파이프라인 A(센서 실측 기반 관제) 스냅샷 요청. SIM SnapshotRequest와 1:1 매칭.
 */
public record SnapshotRequestDto(
        @NotNull
        Long marketId,

        // null이면 SIM이 각 구역의 최신 관측값을 사용
        Instant capturedAt,

        // null이면 false로 취급 (산출된 위험도를 mrkrisk01m에 기록할지 여부)
        Boolean persistRisk,

        // null이면 true로 취급 (개별 에이전트 좌표 포함 여부)
        Boolean includeAgents
) {
}
