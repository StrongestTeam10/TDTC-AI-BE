package com.markettwin.backend.dto.response;

import java.time.Instant;

public record AlertLogDto(
        Long alertId,
        Instant timestamp,
        Long nodeId,
        String alertType,
        String message,
        Boolean resolved
) {
}
