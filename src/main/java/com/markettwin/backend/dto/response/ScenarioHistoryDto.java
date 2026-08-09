package com.markettwin.backend.dto.response;

import java.time.Instant;

/**
 * 2026-07-31 추가
 * 사용자가 실행한 시뮬레이션 이력 한 줄.
 *
 * 보고서가 있는 실행과 없는 실행이 함께 나온다. 보고서 관련 필드(reportTitle,
 * downloadPath)는 hasReport가 true일 때만 채워진다.
 */
public record ScenarioHistoryDto(
        Long scenarioId,

        // 표시용 시나리오명. 저장된 이름이 자동 생성된 타임스탬프 형태면
        // "{시장명} {정책유형} 시나리오 (yyyy-MM-dd HH:mm)"로 대체된다.
        String scenarioName,

        Long marketId,
        String marketName,
        Integer agentCount,
        String policyTypeCode,

        // 시뮬레이션 결과 산출 시각. 목록 정렬 기준이기도 하다.
        Instant executedAt,

        // 이 실행으로 만든 보고서가 있는지. false면 아래 두 필드는 null이다.
        boolean hasReport,

        // 보고서 문서 표지에 실제로 박힌 제목. 보고서가 없으면 null.
        String reportTitle,

        // 다운로드 주소를 발급받을 경로. 보고서가 없으면 null.
        String downloadPath,

        // 2026-08-06 추가: 이 실행을 돌린 사용자 이름.
        // 관리자 전체 목록(GET /api/simulation/scenarios)에서 실행자를 구분하는 데 쓴다.
        // 본인 목록(/my)에도 같이 담기지만 항상 자기 이름이라 화면에서 쓰지 않는다.
        // simscnr01m.user_id가 NULL인 옛 데이터는 null이다.
        String ownerName,

        // 2026-08-06 추가: 이 실행의 종합 위험 점수(0~100).
        // 위험도 계산 이전 데이터는 null이며, 화면은 "-"로 표시한다.
        // 등급(안전/주의/위험/심각) 구분은 화면이 맡는다 - 경계를 바꿀 때 BE를 함께
        // 고쳐야 하는 상황을 만들지 않기 위해 여기서는 점수만 내려준다.
        Integer predictedRiskScore
) {
}
