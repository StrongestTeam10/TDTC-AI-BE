package com.markettwin.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * SENRAD01H - 레이더 센서 데이터 로그
 */
@Entity
@Table(name = "SENRAD01H")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RadarReadingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "crowd_density_sq")
    private Long crowdDensitySq;

    @Column(name = "crowd_density_id", nullable = false)
    private Long radarReadingId;

    @Column(name = "refl_intens", nullable = false)
    private Integer reflIntens;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "detect_cnt")
    private Integer detectCnt;

    @Column(name = "avg_speed")
    private Integer avgSpeed;

    @Column(name = "status_level_code", length = 3)
    private String statusLevelCode;

    @Column(name = "created_at")
    private Instant createdAt;
}
