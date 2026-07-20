package com.markettwin.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * AUDEVNT01H - 음향 이벤트 로그 (AUDEVNT01M의 변경 이력)
 */
@Entity
@Table(name = "audevnt01h")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcousticEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_sq")
    private Long eventSq;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "sound_type", length = 20)
    private String soundType;

    @Column(name = "confidence", precision = 3, scale = 2)
    private BigDecimal confidence;

    @Column(name = "is_checked")
    @Builder.Default
    private Boolean isChecked = false;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(name = "created_at")
    private Instant createdAt;
}
