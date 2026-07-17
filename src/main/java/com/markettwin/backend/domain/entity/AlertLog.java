package com.markettwin.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "alert_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_id")
    private Long alertId;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "node_id", nullable = false)
    private Long nodeId;

    @Column(name = "alert_type", nullable = false, length = 30)
    private String alertType;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "resolved", nullable = false)
    private Boolean resolved;
}
