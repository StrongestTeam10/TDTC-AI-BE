package com.markettwin.backend.controlsystemCCTV.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "mktsmry01s")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketSummary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "summary_id")
    private Long summaryId;

    @Column(name = "market_id", nullable = false)
    private Long marketId;

    @Column(name = "frame_id", nullable = false)
    private Integer frameId;

    @Column(name = "total_cctv_count")
    private Integer totalCctvCount;

    @Column(name = "avg_density_score", precision = 5, scale = 2)
    private BigDecimal avgDensityScore;

    @Column(name = "max_density_score", precision = 5, scale = 2)
    private BigDecimal maxDensityScore;

    @Column(name = "max_risk_score", precision = 5, scale = 2)
    private BigDecimal maxRiskScore;

    @Column(name = "captured_at", nullable = false)
    @Builder.Default
    private Instant capturedAt = Instant.now();

    @Column(name = "analysis_mode", length = 10)
    @Builder.Default
    private String analysisMode = "LIVE";

    @Column(name = "video_id")
    private Long videoId;
}
