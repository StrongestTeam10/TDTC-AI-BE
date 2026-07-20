package com.markettwin.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * SENSENS01M - 센서 (라이다/레이더/음향 공통 마스터)
 */
@Entity
@Table(name = "sensens01m")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sensor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sensor_id")
    private Long sensorId;

    @Column(name = "market_id", nullable = false)
    private Long marketId;

    @Column(name = "zone_id", nullable = false)
    private Long zoneId;

    @Column(name = "sensor_type_code", nullable = false, length = 3)
    private String sensorTypeCode;   // 예: LID(라이다), RAD(레이더), AUD(음향)

    @Column(name = "ip_address", length = 50)
    private String ipAddress;
}
