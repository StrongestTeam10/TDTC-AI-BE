package com.markettwin.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * SENLIDR01M - 라이다 센서 데이터
 * 참고: PK 컬럼명이 ERD상 crowd_density_id로 되어 있으나 이 테이블 고유의 라이다 레코드 식별자임
 */
@Entity
@Table(name = "senlidr01m")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LidarReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "crowd_density_id")
    private Long lidarReadingId;

    @Column(name = "sensor_id", nullable = false)
    private Long sensorId;

    @Column(name = "pt_cloud_cnt", nullable = false)
    private Integer ptCloudCnt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "detect_cnt")
    private Integer detectCnt;

    @Column(name = "avg_dist_m")
    private Integer avgDistM;

    @Column(name = "status_level_code", length = 5)
    private String statusLevelCode;

    @Column(name = "density_score")
    private Integer densityScore;
}
