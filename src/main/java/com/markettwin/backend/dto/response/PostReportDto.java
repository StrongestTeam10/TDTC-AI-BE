package com.markettwin.backend.dto.response;
import com.markettwin.backend.domain.entity.PostReport;
import java.time.Instant;
import java.time.LocalDate;

public record PostReportDto(Long reportId, Long alertId, LocalDate targetDate, String llmSummary, String s3PdfUrl, Instant createdAt, Long videoId) {
    public static PostReportDto from(PostReport entity) {
        return new PostReportDto(entity.getReportId(), entity.getAlertId(), entity.getTargetDate(), entity.getLlmSummary(), entity.getS3PdfUrl(), entity.getCreatedAt(), entity.getVideoId());
    }
}
