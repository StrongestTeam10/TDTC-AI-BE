package com.markettwin.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * MRKFCPH01D - 시설 외관 사진
 *
 * 2026-08-04 추가 (상점 외관 직접 촬영 데이터 수집 파이프라인)
 * 파일 바이너리는 S3에 저장하고, 여기는 메타데이터 + S3 오브젝트 키만 보관
 * (brdattc01d와 동일한 방식). exif_*는 EXIF에서 추출된 원본값(없을 수 있음),
 * corrected_*는 사용자가 지도에서 보정한 최종값(항상 존재).
 */
@Entity
@Table(name = "mrkfcph01d")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacilityPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "photo_id")
    private Long photoId;

    @Column(name = "facility_id", nullable = false)
    private Long facilityId;

    /** comcode01m DIR 도메인(DIRNO/DIREA/DIRSO/DIRWE) - 촬영자가 직접 라벨링 */
    @Column(name = "direction_code", nullable = false, length = 5)
    private String directionCode;

    @Column(name = "s3_key", nullable = false, length = 500)
    private String s3Key;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "exif_latitude", precision = 10, scale = 8)
    private BigDecimal exifLatitude;

    @Column(name = "exif_longitude", precision = 11, scale = 8)
    private BigDecimal exifLongitude;

    @Column(name = "corrected_latitude", nullable = false, precision = 10, scale = 8)
    private BigDecimal correctedLatitude;

    @Column(name = "corrected_longitude", nullable = false, precision = 11, scale = 8)
    private BigDecimal correctedLongitude;

    @Column(name = "captured_at")
    private Instant capturedAt;

    @Column(name = "uploaded_by", nullable = false)
    private Long uploadedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
