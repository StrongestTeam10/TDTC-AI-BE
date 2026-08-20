package com.markettwin.backend.controller;

import com.markettwin.backend.dto.request.MetricBulkRequest;
import com.markettwin.backend.service.MetricBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/metrics")
@RequiredArgsConstructor
public class MetricWebhookController {

    private final MetricBatchService metricBatchService;

    @Value("${ai.secret-key}")
    private String aiSecretKey;

    @PostMapping("/bulk")
    public ResponseEntity<?> receiveBulkMetrics(
            @RequestHeader(value = "X-API-KEY", required = false) String apiKey,
            @RequestBody MetricBulkRequest payload) {

        if (apiKey == null || !apiKey.equals(aiSecretKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "API Key 인증 실패"));
        }

        try {
            Map<String, Object> result = metricBatchService.bulkInsertMetrics(payload);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            if ("ALREADY_EXISTS".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "이미 적재된 분량(clip_id + frame_id 중복)입니다."));
            }
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
