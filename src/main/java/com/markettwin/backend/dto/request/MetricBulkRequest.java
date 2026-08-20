package com.markettwin.backend.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class MetricBulkRequest {
    private Long zoneId;
    private Long clipId;
    private List<FrameMetricDto> frames;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class FrameMetricDto {
        private Integer frameId;
        private Long videoId;
        private String pixelsJson;
        private String bevXyzJson;
        private String capturedAt;
        private Integer totalCount;
        private Double riskScore;
        private String riskLevel;
        private String reasonCode;
        private Double occupancyRate;
        private Double stagnationSec;
        private String videoUrl;
    }
}
