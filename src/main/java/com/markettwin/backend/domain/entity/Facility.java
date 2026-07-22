package com.markettwin.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * MRKFCTS01M - 시설
 */
@Entity
@Table(name = "mrkfcts01m")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Facility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "facility_id")
    private Long facilityId;

    @Column(name = "market_id", nullable = false)
    private Long marketId;

    @Column(name = "facility_type", nullable = false, length = 50)
    private String facilityType;   // 위험/화재변 등

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /** 시설 위치 위도 (WGS84) - 출입구 등 시뮬레이션에서 좌표가 필요한 시설용 */
    @Column(name = "latitude", precision = 10, scale = 8)
    private java.math.BigDecimal latitude;

    /** 시설 위치 경도 (WGS84) */
    @Column(name = "longitude", precision = 11, scale = 8)
    private java.math.BigDecimal longitude;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
