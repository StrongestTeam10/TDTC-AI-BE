package com.markettwin.backend.dto.response;

import com.markettwin.backend.dto.request.CorridorPolicyDto;
import com.markettwin.backend.dto.request.PlacedObjectDto;

import java.time.Instant;
import java.util.List;

/**
 * (시장 오브젝트/구조 설정 조회 응답).
 * objects/corridorPolicies는 시뮬레이션 요청과 같은 형식이라, FE가 초기 배치로
 * 그대로 시뮬레이션 비교에 넘길 수 있다.
 */
public record MarketObjectConfigDto(
        Long marketId,
        List<PlacedObjectDto> objects,
        List<CorridorPolicyDto> corridorPolicies,
        Instant updatedAt
) {
}
