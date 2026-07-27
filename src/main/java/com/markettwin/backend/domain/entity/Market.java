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

    // 2026-07-27 추가: 담당 시장/구역별 권한 분리(상인회·지자체는 본인 담당 시장만
    // 조회, 관리자는 전체 + 시장 전환)를 위해 usrusrs01m.market_code / brdpsts01m.market_code와
    // 동일한 comcode01m MKT 도메인 코드를 mrkaddr01m에도 부여함. 게시판이 marketCode(comcode)
    // 문자열로 직접 필터링하는 것과 같은 방식으로, 대시보드도 이 필드로 마켓 소유권을 판정한다.
    @Column(name = "market_code", length = 5)
    private String marketCode;
}
