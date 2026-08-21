package com.markettwin.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * (시장 오브젝트/구조 설정 저장). 시장당 1세트를 통째로 덮어쓴다.
 * objects/corridorPolicies가 null이면 서비스단에서 빈 배열로 취급한다.
 */
@Getter
@NoArgsConstructor
public class MarketObjectConfigSaveRequestDto {

    @NotNull(message = "시장은 필수입니다.")
    private Long marketId;

    @Valid
    private List<PlacedObjectDto> objects;

    @Valid
    private List<CorridorPolicyDto> corridorPolicies;
}
