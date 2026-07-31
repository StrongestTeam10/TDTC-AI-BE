package com.markettwin.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * MRKADDR01M - 시장 위치
 */
@Entity
@Table(name = "mrkaddr01m")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Market {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "market_id")
    private Long marketId;

    @Column(name = "market_name", nullable = false, length = 50)
    private String marketName;

    @Column(name = "latitude", precision = 10, scale = 8)
    private java.math.BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private java.math.BigDecimal longitude;

    // 2026-07-27 추가(ERD 반영): 시장 구분 코드(comcode01m MKT 도메인)
    @Column(name = "market_code", nullable = false, length = 5)
    private String marketCode;
}
