package com.markettwin.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 시설 관리 화면 - 상점 위치 등록
 */
@Getter
@Builder
@AllArgsConstructor
public class FacilityDto {
    private Long facilityId;
    private Long marketId;
    private String facilityType;
    private String name;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Boolean isActive;
    private String rmk;
    private long photoCount;
    private Instant updatedAt;
}
