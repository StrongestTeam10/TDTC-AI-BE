package com.markettwin.backend.dto.response;

/**
 * 보고서 생성 결과.
 *
 * downloadUrl은 만료 시간이 있는 presigned URL이다. 만료 후 다시 받으려면
 * GET /api/simulation/reports/{scenarioId}/download 로 재발급하면 되고,
 * 보고서를 다시 생성할 필요는 없다.
 */
public record ReportGenerateResponseDto(
        String reportId,
        Long scenarioId,
        String downloadUrl,
        String storageKey
) {
}
