package com.markettwin.backend.controller;
import com.markettwin.backend.dto.response.PostReportDto;
import com.markettwin.backend.repository.PostReportRepository;
import com.markettwin.backend.service.VideoS3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/v1/post-reports")
@RequiredArgsConstructor
public class PostReportController {

    private final PostReportRepository repository;
    private final VideoS3Service videoS3Service; // 🌟 서비스 주입

    @GetMapping
    public List<PostReportDto> getAllPostReports() {
        return repository.findAll().stream().map(report -> {
            // 🌟 널(Null) 체크 후 안전하게 URL 변환
            String viewUrl = (report.getS3PdfUrl() != null && !report.getS3PdfUrl().isBlank())
                    ? videoS3Service.generatePresignedDownloadUrl(report.getS3PdfUrl(), Duration.ofHours(1)).toString()
                    : null;
            return PostReportDto.from(report, viewUrl);
        }).toList();
    }
}
