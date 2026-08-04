package com.markettwin.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "vdoclip01m")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoClip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "clip_id")
    private Long clipId;

    @Column(name = "zone_id", nullable = false)
    private Long zoneId;

    @Column(name = "clip_type", nullable = false, length = 20)
    private String clipType;

    @Column(name = "s3_clip_url", nullable = false, columnDefinition = "text")
    private String s3ClipUrl;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "is_downloaded", nullable = false)
    @Builder.Default
    private Boolean isDownloaded = false;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "factor_id")
    private Long factorId;
}
