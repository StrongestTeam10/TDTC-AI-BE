package com.markettwin.backend.dto.response;
import com.markettwin.backend.domain.entity.VideoClip;
import java.time.Instant;

public record VideoClipDto(Long clipId, Long zoneId, String clipType, String s3ClipUrl, Instant startTime, Instant endTime, Boolean isDownloaded, Boolean isDeleted, Instant expiresAt, Long factorId) {
    public static VideoClipDto from(VideoClip entity) {
        return new VideoClipDto(entity.getClipId(), entity.getZoneId(), entity.getClipType(), entity.getS3ClipUrl(), entity.getStartTime(), entity.getEndTime(), entity.getIsDownloaded(), entity.getIsDeleted(), entity.getExpiresAt(), entity.getFactorId());
    }
}
