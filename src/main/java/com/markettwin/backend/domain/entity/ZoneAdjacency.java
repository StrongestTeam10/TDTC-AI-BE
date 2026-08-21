package com.markettwin.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * MRKADJC01M - 시장 구역 인접 관계 (통로 연결 그래프)
 *
 * Mesa NetworkGrid 구성 및 유동인구 이동 경로 계산의 기반 데이터.
 * from_zone_id -> to_zone_id 방향으로 저장하며, 양방향 통로는 두 행으로 표현한다.
 *
 * market_id 컬럼 제거(from_zone_id/to_zone_id가 속한 구역을 통해
 * mrkaddr01d.market_id로 알 수 있어 중복이었음 - MarketService.getCorridors() 등에서
 * 시장별로 조회할 땐 이제 ZoneAdjacencyRepository가 Zone과 조인해서 필터링함).
 * createdAt 추가.
 * 한때 테이블명을 mrkadjs01m으로 리네임했으나 오기로 확인되어 mrkadjc01m 유지.
 */
@Entity
@Table(name = "mrkadjc01m")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ZoneAdjacency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "adjacency_id")
    private Long adjacencyId;

    @Column(name = "from_zone_id", nullable = false)
    private Long fromZoneId;

    @Column(name = "to_zone_id", nullable = false)
    private Long toZoneId;

    /** 통로 폭(m) - 병목 및 수용 인원 계산에 사용 */
    @Column(name = "path_width", precision = 4, scale = 2)
    private BigDecimal pathWidth;

    /** 구역 중심 간 거리(m) - 이동 시간 산출에 사용 */
    @Column(name = "distance_m", precision = 6, scale = 2)
    private BigDecimal distanceM;

    /** 통행 가능 여부 - 통로 폐쇄 시나리오 시 false로 전환 */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * 통로 중심선(GeoJSON LineString, WGS84 [경도,위도] 순서).
     * 레이아웃 에디터로 실제 통로를 따라 그린 선. null이면 SIM이 두 구역이 맞닿은
     * 경계 중점 1개로 근사해서 대체한다(정확도는 떨어짐).
     */
    @Column(name = "path_coordinates", columnDefinition = "TEXT")
    private String pathCoordinates;

    @Column(name = "created_at")
    private Instant createdAt;
}
