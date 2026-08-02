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

    /**
     * 2026-07-24 추가: GATE는 유입 가중치(클수록 신규 방문객이 더 많이 유입),
     * STALL 등 그 외 타입은 매력도 가중치(클수록 방문객을 더 끌어당김)로 쓰인다.
     */
    @Column(name = "weight")
    @Builder.Default
    private Double weight = 1.0;

    /**
     * 2026-07-24 추가: 오브젝트(매대/푸드트럭 등)가 실제로 차지하는 반경(m).
     * SIM이 이 값만큼 걸어다닐 수 없는 장애물로 취급해서 유동인구 이동 경로가
     * 이 오브젝트를 피해가도록 만든다. GATE는 의미 없으니 null로 둬도 됨. 값이
     * 없으면 SIM이 임시 기본값(현재 1.2m)으로 대체한다.
     */
    @Column(name = "footprint_radius_m")
    private Double footprintRadiusM;

    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * 매력도(weight) 업데이트 및 갱신 시간 기록
     */
    public void updateWeight(Double weight) {
        this.weight = weight;
        this.updatedAt = Instant.now();
    }
}
