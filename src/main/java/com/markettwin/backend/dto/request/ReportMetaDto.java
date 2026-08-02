package com.markettwin.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * SIM ReportMeta와 1:1. 보고서 문서 메타데이터.
 *
 * reportTitle이 null이면 SIM이 "OO시장 OO 디지털 트윈 시뮬레이션 결과 보고서" 형태로
 * 제목을 만든다. NON_NULL이라 null 필드는 JSON에서 아예 빠지고 SIM 쪽 기본값이 적용된다.
 * 면책 문구(disclaimer)는 호출자마다 다를 이유가 없어 SIM이 자체 관리하므로 보내지 않는다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ReportMetaDto(
        String reportId,
        String reportTitle,
        String decisionQuestion
) {
}
