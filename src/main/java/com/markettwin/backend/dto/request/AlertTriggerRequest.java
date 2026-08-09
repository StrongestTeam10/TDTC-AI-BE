package com.markettwin.backend.dto.request;

public record AlertTriggerRequest(
        Long zoneId,
        String alertType,
        String pdfUrl,
        String videoUrl,
        String llmSummary
) {}
