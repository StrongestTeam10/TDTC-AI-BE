package com.markettwin.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * SENLIDR01H - 라이다 센서 데이터 로그
 */
@Entity
@Table(name = "senlidr01h")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LidarReadingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "crowd_density_sq")
    private Long crowdDensitySq;

    @Column(name = "crowd_density_id", nullable = false)
    private Long lidarReadingId;

    @Column(name = "pt_cloud_cnt", nullable = false)
    private Integer ptCloudCnt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "detect_cnt")
    private Integer detectCnt;

    @Column(name = "avg_dist_m")
    private Integer avgDistM;

    @Column(name = "status_level_code", length = 3)
    private String statusLevelCode;

    @Column(name = "density_score")
    private Integer densityScore;

    @Column(name = "created_at")
    private Instant createdAt;
}
