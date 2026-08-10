package com.markettwin.backend.controller;

import com.markettwin.backend.domain.entity.PostReport;
import com.markettwin.backend.dto.request.PostReportWebhookRequest;
import com.markettwin.backend.dto.response.PostReportDto;
import com.markettwin.backend.repository.PostReportRepository;
import com.markettwin.backend.service.VideoS3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
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

    private final PostReportRepository repository;
    private final VideoS3Service videoS3Service;

    @Value("${ai.secret-key}")
    private String aiSecretKey;

    @GetMapping
    public List<PostReportDto> getAllPostReports() {
        return repository.findAll().stream().map(report -> {
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

        if (apiKey == null || !apiKey.equals(aiSecretKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("❌ 경고: API 키가 일치하지 않거나 누락되었습니다.");
        }

        System.out.println("✅ 파이썬으로부터 PDF 명세서 URL 수신 완료: " + payload.getS3PdfUrl());

        PostReport report = PostReport.builder()
                .alertId(payload.getAlertId())
                .targetDate(LocalDate.now())
                .llmSummary(payload.getLlmSummary())
                .s3PdfUrl(payload.getS3PdfUrl())
                .videoId(payload.getVideoId())
                .build();

        repository.save(report);

        return ResponseEntity.ok().body("PDF 명세서 DB 등록 완료");
    }
}
