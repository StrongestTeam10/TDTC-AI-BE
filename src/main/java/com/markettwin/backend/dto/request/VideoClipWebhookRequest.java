package com.markettwin.backend.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VideoClipWebhookRequest {
    private Long zoneId;
    private String clipType;
    private String s3ClipUrl;
    private String startTime;
    private String endTime;
}
