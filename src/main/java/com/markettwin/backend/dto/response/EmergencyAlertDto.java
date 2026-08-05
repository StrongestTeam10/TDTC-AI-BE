package com.markettwin.backend.dto.response;
import com.markettwin.backend.domain.entity.EmergencyAlert;
import java.time.Instant;

public record EmergencyAlertDto(Long alertId, Long zoneId, String alertType, Boolean isResolved, Instant alertedAt) {
    public static EmergencyAlertDto from(EmergencyAlert entity) {
        return new EmergencyAlertDto(entity.getAlertId(), entity.getZoneId(), entity.getAlertType(), entity.getIsResolved(), entity.getAlertedAt());
    }
}
