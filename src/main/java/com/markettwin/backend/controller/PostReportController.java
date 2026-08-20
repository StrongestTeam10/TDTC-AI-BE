package com.markettwin.backend.controller;

import com.markettwin.backend.domain.entity.PostReport;
import com.markettwin.backend.dto.request.PostReportWebhookRequest;
import com.markettwin.backend.dto.response.PostReportDto;
import com.markettwin.backend.repository.PostReportRepository;
import com.markettwin.backend.service.VideoS3Service;
import com.markettwin.backend.util.ApiKeys;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/post-reports")
@RequiredArgsConstructor
public class PostReportController {

    private static final Logger log = LoggerFactory.getLogger(PostReportController.class);

    private final PostReportRepository repository;
    private final VideoS3Service videoS3Service;

    @Value("${ai.secret-key}")
    private String aiSecretKey;

    @GetMapping
    public List<PostReportDto> getAllPostReports(@RequestParam(required = false) Long zoneId) {
        // ★ 추가됨: zoneId가 넘어오면 네이티브 조인 쿼리로 필터링, 없으면 전체 조회
        List<PostReport> reports = (zoneId != null)
                ? repository.findByZoneIdNative(zoneId)
                : repository.findAll();

        return reports.stream().map(report -> {
            String viewUrl = (report.getS3PdfUrl() != null && !report.getS3PdfUrl().isBlank())
                    ? videoS3Service.generatePresignedDownloadUrl(report.getS3PdfUrl(), Duration.ofHours(1)).toString()
                    : null;
            return PostReportDto.from(report, viewUrl);
        }).toList();
    }

    @PostMapping
    public ResponseEntity<?> receivePdfReportWebhook(
            @RequestHeader(value = "X-API-KEY", required = false) String apiKey,
            @RequestBody PostReportWebhookRequest payload) {

        if (!ApiKeys.matches(apiKey, aiSecretKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("❌ 경고: API 키가 일치하지 않거나 누락되었습니다.");
        }

        PostReport report = PostReport.builder()
                .alertId(payload.getAlertId())
                .targetDate(LocalDate.now())
                .llmSummary(payload.getLlmSummary())
                .s3PdfUrl(payload.getS3PdfUrl())
                .videoId(payload.getVideoId())
                .build();

        PostReport saved = repository.save(report);

        // S3 URL은 남기지 않는다 - 서명 없는 객체 경로라도 버킷 구조가 로그로 새어
        // 나가고, 외부(AI 서버)에서 온 문자열이라 그대로 찍으면 로그 위조에 쓰인다.
        // 어느 신고가 들어왔는지 추적하는 데는 식별자로 충분하다.
        log.info("AI PDF 명세서 수신: reportId={}, alertId={}, videoId={}",
                saved.getReportId(), saved.getAlertId(), saved.getVideoId());

        return ResponseEntity.ok().body("PDF 명세서 DB 등록 완료");
    }
}
