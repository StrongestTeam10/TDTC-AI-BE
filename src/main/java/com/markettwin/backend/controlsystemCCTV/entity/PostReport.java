package com.markettwin.backend.controlsystemCCTV.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "pstrprt01h")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    @Column(name = "zone_id", nullable = false)
    private Long zoneId;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(name = "llm_summary", columnDefinition = "text")
    private String llmSummary;

    @Column(name = "s3_pdf_url", columnDefinition = "text")
    private String s3PdfUrl;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "analysis_mode", length = 10)
    @Builder.Default
    private String analysisMode = "LIVE";

    @Column(name = "video_id")
    private Long videoId;
}
