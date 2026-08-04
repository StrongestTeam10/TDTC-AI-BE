package com.markettwin.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.Instant;

/**
 * SIM ScenarioRow와 1:1. 시나리오 한 행.
 *
 * 대안(simscnr01m)과 현행안(simbsln01m) 모두 이 모양으로 보낸다.
 * 현행안에는 created_at 컬럼이 없어 null로 나가며, SIM에서 선택 항목이다.
 *
 * virtualConfig는 DB에 JSON 문자열로 들어 있고 SIM이 문자열도 받아서 파싱하므로
 * String 그대로 넘긴다. 중간에 객체로 파싱했다가 다시 문자열로 만들면
 * 이중 인코딩되니 주의.
 *
 * space_mod_data는 보내지 않는다. SIM이 보고서의 "변경사항"을 virtual_config의
 * objects/events/corridorPolicies/closedGateIds에서 직접 파생하기 때문이다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ScenarioRowDto(
        Long scenarioId,
        String scenarioName,
        Long marketId,
        String virtualConfig,
        Instant regDatetime,
        Integer agentCount,
        String policyTypeCode,
        Instant createdAt
) {
}
