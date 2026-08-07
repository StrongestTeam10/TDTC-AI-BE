package com.markettwin.backend.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostReportWebhookRequest {
    private Long alertId;
    private String llmSummary;
    private String s3PdfUrl;
    private Long videoId;
}
