package com.markettwin.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * MRKADJC01M - 시장 구역 인접 관계 (통로 연결 그래프)
 *
 * Mesa NetworkGrid 구성 및 유동인구 이동 경로 계산의 기반 데이터.
 * from_zone_id -> to_zone_id 방향으로 저장하며, 양방향 통로는 두 행으로 표현한다.
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

    @Column(name = "market_id", nullable = false)
    private Long marketId;

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
}
