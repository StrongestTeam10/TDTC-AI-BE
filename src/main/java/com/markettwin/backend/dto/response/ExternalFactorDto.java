package com.markettwin.backend.dto.response;
import com.markettwin.backend.domain.entity.ExternalFactor;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ExternalFactorDto(Long factorId, Long marketId, LocalDate targetDate, String weatherCondition, BigDecimal temperature, String eventCategory, String eventName, Instant updatedAt, Long videoId) {
    public static ExternalFactorDto from(ExternalFactor entity) {
        return new ExternalFactorDto(entity.getFactorId(), entity.getMarketId(), entity.getTargetDate(), entity.getWeatherCondition(), entity.getTemperature(), entity.getEventCategory(), entity.getEventName(), entity.getUpdatedAt(), entity.getVideoId());
    }
}
