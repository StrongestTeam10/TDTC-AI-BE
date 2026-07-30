package com.markettwin.backend.controlsystemCCTV.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "pedcord01h")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedestrianCoordinate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coord_id")
    private Long coordId;

    @Column(name = "zone_id", nullable = false)
    private Long zoneId;

    @Column(name = "frame_id", nullable = false)
    private Integer frameId;

    @Column(name = "person_id", nullable = false)
    private Long personId;

    @Column(name = "pixel_x", precision = 7, scale = 2)
    private BigDecimal pixelX;

    @Column(name = "pixel_y", precision = 7, scale = 2)
    private BigDecimal pixelY;

    @Column(name = "bev_x_m", precision = 6, scale = 3)
    private BigDecimal bevXM;

    @Column(name = "bev_y_m", precision = 6, scale = 3)
    private BigDecimal bevYM;

    @Column(name = "risk_score", precision = 5, scale = 2)
    private BigDecimal riskScore;

    @Column(name = "captured_at", nullable = false)
    @Builder.Default
    private Instant capturedAt = Instant.now();

    @Column(name = "analysis_mode", length = 10)
    @Builder.Default
    private String analysisMode = "LIVE";

    @Column(name = "video_id")
    private Long videoId;
}
