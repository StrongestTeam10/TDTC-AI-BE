package com.markettwin.backend.controlsystemCCTV.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Entity
@Table(name = "pedaggr01h")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedestrianCoordinateJson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coord_id")
    private Long coordId;

    @Column(name = "zone_id", nullable = false)
    private Long zoneId;

    @Column(name = "frame_id", nullable = false)
    private Integer frameId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pixels_json", columnDefinition = "jsonb")
    private String pixelsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "bev_xyz_json", columnDefinition = "jsonb")
    private String bevXyzJson;

    @Column(name = "captured_at", nullable = false)
    @Builder.Default
    private Instant capturedAt = Instant.now();

    @Column(name = "analysis_mode", length = 10)
    @Builder.Default
    private String analysisMode = "LIVE";

    @Column(name = "video_id")
    private Long videoId;
}
