package com.markettwin.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * AUDEVNT01M - 음향 이벤트 분석
 */
@Entity
@Table(name = "audevnt01m")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcousticEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "sensor_id", nullable = false)
    private Long sensorId;

    @Column(name = "sound_type", length = 20)
    private String soundType;   // 비명, 충돌 등

    @Column(name = "confidence", precision = 3, scale = 2)
    private BigDecimal confidence;

    @Column(name = "is_checked")
    @Builder.Default
    private Boolean isChecked = false;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;
}
