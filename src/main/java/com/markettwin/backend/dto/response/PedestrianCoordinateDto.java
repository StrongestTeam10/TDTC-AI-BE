package com.markettwin.backend.dto.response;

import com.markettwin.backend.domain.entity.PedestrianCoordinateJson;
import java.time.Instant;

public record PedestrianCoordinateDto(
        Long coordId,
        Integer frameId,
        String pixelsJson,
        String bevXyzJson,
        Instant capturedAt,
        Long videoId,
        Long clipId,
        Long zoneId
) {
    public static PedestrianCoordinateDto from(PedestrianCoordinateJson entity) {
        return new PedestrianCoordinateDto(
                entity.getCoordId(),
                entity.getFrameId(),
                entity.getPixelsJson(),
                entity.getBevXyzJson(),
                entity.getCapturedAt(),
                entity.getVideoId(),
                entity.getClipId(),
                entity.getZoneId()
        );
    }
}
