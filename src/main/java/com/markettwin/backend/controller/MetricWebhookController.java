package com.markettwin.backend.controller;

import com.markettwin.backend.dto.request.MetricBulkRequest;
import com.markettwin.backend.service.MetricBatchService;
import com.markettwin.backend.util.ApiKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI 파이프라인이 보내는 1분 단위 보행자 메트릭을 벌크로 적재한다.
 *
 * SecurityConfig에서 permitAll이며 인증은 X-API-KEY(ai.secret-key)로 직접 한다.
 * 다른 AI 웹훅(알람·클립·보고서)과 같은 방식이다.
 */
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

        // 키가 설정되지 않았으면 빈 헤더도 거부한다(ApiKeys 참고).
        if (!ApiKeys.matches(apiKey, aiSecretKey)) {
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
