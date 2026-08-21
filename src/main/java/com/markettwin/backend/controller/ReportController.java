package com.markettwin.backend.controller;

import com.markettwin.backend.dto.request.ReportGenerateRequestDto;
import com.markettwin.backend.dto.response.ReportDownloadDto;
import com.markettwin.backend.dto.response.ReportGenerateResponseDto;
import com.markettwin.backend.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 보고서 기능
 * 시뮬레이션 결과를 현행안과 비교해 정책 보고서(DOCX)를 생성한다.
 */
@RestController
@RequestMapping("/api/simulation/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /** 보고서를 생성하고 다운로드용 presigned URL을 반환한다. */
    @PostMapping
    public ReportGenerateResponseDto generate(
            @Valid @RequestBody ReportGenerateRequestDto request) {
        return reportService.generate(request);
    }

    /**
     * 이미 생성된 보고서의 다운로드 주소를 새로 발급한다.
     *
     * presigned URL은 수명이 짧아 DB에 담아둘 수 없으므로 S3 키만 저장해 두고 여기서
     * 주소만 다시 만든다. 파일은 S3에 그대로 있어 보고서를 재생성하지 않는다.
     * 재생성은 RAG 검색 + LLM 생성까지 다시 도는 수 분짜리 작업이다
     *
     * 302 리다이렉트 대신 JSON으로 URL을 돌려준다. 이 API는 JWT가 필요한데 브라우저가
     * 단순 이동하면 Authorization 헤더가 실리지 않고, fetch로 부르면 리다이렉트를 따라
     * S3로 갈 때 CORS에 막힌다. 클라이언트는 받은 URL로 직접 이동하면 된다.
     */
    @GetMapping("/{scenarioId}/download")
    public ReportDownloadDto download(@PathVariable Long scenarioId) {
        return reportService.reissueDownloadUrl(scenarioId);
    }
}
