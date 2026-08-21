package com.markettwin.backend.dto.response;

import com.markettwin.backend.domain.entity.CctvZone;

import java.time.Instant;

/**
 * (CCTV 관제 구역). 2차: zoneName은 zoneId로 MRKADDR01D를
 * 조인해 채운다(이 테이블 자체엔 구역명이 없음).
 */
public record CctvZoneDto(
        Long cctvZoneId,
        Long marketId,
        Long zoneId,
        /** MRKADDR01D를 조인해 채우는 소속 시뮬레이션 구역명 */
        String zoneName,
        Boolean isActive,
        String polygonCoordinates,
        String rmk,
        Instant updatedAt
) {
    public static CctvZoneDto from(CctvZone entity, String zoneName) {
        return new CctvZoneDto(
                entity.getCctvZoneId(),
                entity.getMarketId(),
                entity.getZoneId(),
                zoneName,
                entity.getIsActive(),
                entity.getPolygonCoordinates(),
                entity.getRmk(),
                entity.getUpdatedAt()
        );
    }
}
