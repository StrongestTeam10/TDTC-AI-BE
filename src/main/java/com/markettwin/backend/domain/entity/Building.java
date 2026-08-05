package com.markettwin.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * MRKBLDG01M - 상가/건물 폴리곤. 시뮬레이션 계산에는 쓰이지 않고
 * 지도 위에 3D 느낌의 건물 형태를 표시하는 용도.
 *
 * 2026-08-XX: zone_id(남측/중앙/북측 등 시장 구역) 컬럼은 제거했다.
 * 건물을 구역 단위로 묶어 다룰 계획이 없어 자동 배정 로직도 같이 뺐다.
 */
@Entity
@Table(name = "mrkbldg01m")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Building {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "building_id")
    private Long buildingId;

    @Column(name = "market_id", nullable = false)
    private Long marketId;

    /** 필지 식별자(PNU). 19자리+접미사(예: "...038b")가 있어 문자열로 다룬다. */
    @Column(name = "pnu_code", nullable = false, length = 30)
    private String pnuCode;

    /** GeoJSON Polygon, WGS84 [경도,위도] 순 (Zone.polygonCoordinates와 동일 형식) */
    @Column(name = "polygon_coordinates", columnDefinition = "TEXT", nullable = false)
    private String polygonCoordinates;

    @Column(name = "height_m", precision = 6, scale = 2, nullable = false)
    private BigDecimal heightM;

    @Column(name = "height_estimated", nullable = false)
    private Boolean heightEstimated;

    @Column(name = "floors", nullable = false)
    private Integer floors;

    @Column(name = "created_at")
    private Instant createdAt;
}