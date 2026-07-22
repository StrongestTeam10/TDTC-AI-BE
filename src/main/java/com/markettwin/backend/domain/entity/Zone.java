package com.markettwin.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * MRKADDR01D - 시장 구역 위치 좌표
 */
@Entity
@Table(name = "mrkaddr01d")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Zone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "zone_id")
    private Long zoneId;

    @Column(name = "market_id", nullable = false)
    private Long marketId;

    @Column(name = "zone_name", nullable = false, length = 50)
    private String zoneName;

    @Column(name = "polygon_coordinates", columnDefinition = "TEXT")
    private String polygonCoordinates;
}
