package com.markettwin.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * SENRAD01M - 레이더 센서 데이터
 */
@Entity
@Table(name = "SENRAD01M")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RadarReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "crowd_density_id")
    private Long radarReadingId;

    @Column(name = "sensor_id", nullable = false)
    private Long sensorId;

    @Column(name = "refl_intens", nullable = false)
    private Integer reflIntens;   // 레이더 신호 강도

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "detect_cnt")
    private Integer detectCnt;

    @Column(name = "avg_speed")
    private Integer avgSpeed;

    @Column(name = "status_level_code", length = 3)
    private String statusLevelCode;
}
