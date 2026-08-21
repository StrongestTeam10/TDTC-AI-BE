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

    // 추가(ERD 반영): 시장 구분 코드(comcode01m MKT 도메인)
    @Column(name = "market_code", nullable = false, length = 5)
    private String marketCode;

    /**
     * 이름과 중심 좌표만 바꾼다.
     *
     * marketCode를 뺀 것은 일부러다. 코드는 시장의 신원이고 담당 시장 권한이 그것으로
     * 갈려서(MarketService.getAccessibleMarket), 바뀌면 그 시장 담당자가 자기 시장에
     * 못 들어가게 된다. 메서드 시그니처에서부터 못 바꾸게 막아둔다.
     */
    public void updateInfo(String marketName, java.math.BigDecimal latitude, java.math.BigDecimal longitude) {
        this.marketName = marketName;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
