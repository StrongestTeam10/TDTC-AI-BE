package com.markettwin.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "cctv_detections")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CctvDetection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detection_id")
    private Long detectionId;

    @Column(name = "camera_id", nullable = false)
    private Integer cameraId;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "node_id", nullable = false)
    private Long nodeId;

    @Column(name = "person_count", nullable = false)
    private Integer personCount;

    @Column(name = "avg_velocity")
    private Double avgVelocity;
}
