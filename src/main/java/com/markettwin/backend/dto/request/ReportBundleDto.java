package com.markettwin.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

/**
 * SIM DbReportBundle과 1:1. BE가 여러 테이블 조회 결과를 조립해 보내는 요청 묶음.
 *
 * 기준안(현행안)을 별도 필드로 보내는 이유: 현행안은 SIMBSLN01M/01D, 대안은
 * SIMSCNR01M/SIMRSLT01D에서 오는데 두 테이블의 ID가 각각 1부터 시작해서
 * 한 리스트에 섞으면 scenario_id가 충돌한다.
 *
 * density_timeseries_rows는 일부러 넣지 않았다. SIM에서 기본값 빈 리스트로 처리되고,
 * 비어 있으면 시간대별 밀집도 차트 한 장만 빠진 채 나머지는 정상 생성된다.
 * BE에 대응 테이블이 생기면 그때 필드를 추가하면 된다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ReportBundleDto(
        ReportMetaDto reportMeta,
        MarketInfoDto market,

        // 구역 번호를 이름으로 바꿔 쓰기 위한 대조표. 비면 SIM이 "N구역"으로 표기한다.
        List<ZoneInfoDto> zones,

        ScenarioRowDto baselineScenario,
        ScenarioResultRowDto baselineResult,
        List<ScenarioRowDto> scenarioRows,
        List<ScenarioResultRowDto> resultRows
) {
}
