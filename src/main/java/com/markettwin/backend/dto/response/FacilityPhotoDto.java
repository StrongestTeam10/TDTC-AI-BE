package com.markettwin.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.net.URL;
import java.time.Instant;

/**
 * 2026-08-04 추가 (상점 외관 직접 촬영 데이터 수집 파이프라인)
 */
@Getter
@Builder
@AllArgsConstructor
public class FacilityPhotoDto {
    private Long photoId;
    private Long facilityId;
    private String directionCode;
    private String originalName;
    /** presigned GET URL - 목록 조회 시점에 매번 새로 발급 */
    private URL downloadUrl;
    private BigDecimal exifLatitude;
    private BigDecimal exifLongitude;
    private BigDecimal correctedLatitude;
    private BigDecimal correctedLongitude;
    private Instant capturedAt;
    private Instant createdAt;
}
