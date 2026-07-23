package com.markettwin.backend.dto.response;

/**
 * 구역 단위 위험도 세부 지표. SIM RiskBreakdown과 필드명 1:1 일치.
 *
 * 2026-07-23: 레이더(flow)/음향(acoustic) 센서를 완전히 제거하기로 결정하면서
 * 해당 필드도 삭제. density/bottleneck 2개만 남음.
 */
public record RiskBreakdownDto(
        Double density,
        Double bottleneck
) {
}
