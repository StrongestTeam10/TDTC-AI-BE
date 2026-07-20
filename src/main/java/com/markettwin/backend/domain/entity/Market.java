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

    @Column(name = "latitude", precision = 10, scale = 6)
    private java.math.BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 6)
    private java.math.BigDecimal longitude;
}
