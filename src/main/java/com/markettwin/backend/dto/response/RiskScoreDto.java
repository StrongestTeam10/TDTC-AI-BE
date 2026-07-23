package com.markettwin.backend.dto.response;

import java.time.Instant;

/**
 * ⚠️ 이 DTO는 파이프라인 B(시나리오, 팀원 담당) 응답 계약입니다.
 * 2026-07-23 레이더/음향 센서 완전 제거 결정으로 SIM ContributingFactors가
 * density/bottleneck만 보내도록 바뀌어서 부득이 함께 수정했습니다. 담당 팀원에게
 * 꼭 공유해주세요 (FE ScenarioPage 등에서 acoustic/flowRate를 참조하는 곳이 있다면
 * 같이 수정 필요).
 */
public record RiskScoreDto(
        Instant timestamp,
        Double score,           // 0 ~ 100
        String level,           // "low" | "medium" | "high" | "critical"
        ContributingFactors contributingFactors
) {
    public record ContributingFactors(
            Double density,
            Double bottleneck
    ) {
    }
}
