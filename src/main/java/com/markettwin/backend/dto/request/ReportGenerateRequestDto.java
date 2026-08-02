package com.markettwin.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 보고서 생성 요청. FE는 대안 시나리오 하나만 지목하면 되고,
 * 비교 기준(현행안)과 나머지 데이터는 BE가 DB에서 조회해 조립한다.
 *
 * 보고서 1건 = 현행안 1개 vs 대안 1개.
 * SIM은 대안 N개를 받을 수 있지만, 한 시나리오가 여러 보고서에 속하면
 * simrslt01d.generated_report_path(단일 컬럼)로 이력을 표현할 수 없어 1개로 제한한다.
 */
public record ReportGenerateRequestDto(

        /**
         * 보고서로 만들 대안 시나리오(simscnr01m).
         * 비교할 현행안은 이 시나리오의 market_id로 BE가 찾는다.
         */
        @NotNull
        Long scenarioId,

        /**
         * null이면 SIM이 "OO시장 OO 디지털 트윈 시뮬레이션 결과 보고서" 형태로 자동 생성한다.
         *
         * 길이를 simrslt01d.report_title(VARCHAR(200))에 맞춰 제한한다. 이 값이 그대로
         * 문서 제목이 되고 생성 후 DB에 저장되는데, 넘치면 S3 업로드까지 끝난 뒤
         * DB 갱신에서 실패해 참조 없는 파일만 남는다.
         */
        @Size(max = 200)
        String reportTitle,

        /** 보고서가 답해야 할 질문. null이면 SIM 기본 문구가 쓰인다. */
        String decisionQuestion
) {
}
