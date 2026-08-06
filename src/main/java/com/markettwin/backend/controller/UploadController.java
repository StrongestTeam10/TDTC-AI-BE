package com.markettwin.backend.controller;

import com.markettwin.backend.service.VideoS3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
public class UploadController {

    private final VideoS3Service videoS3Service;

    @GetMapping("/presigned-url")
    public ResponseEntity<Map<String, String>> getPresignedUrl(
            @RequestParam String filename,
            @RequestParam(defaultValue = "raw-videos") String folder) {

        //  보안 검증: 원본영상, 위험클립, PDF신고서 3개 폴더만 허용
        if (!folder.equals("raw-videos") && !folder.equals("danger-clips") && !folder.equals("post-reports")) {
            throw new IllegalArgumentException("허용되지 않은 S3 폴더명입니다.");
        }

        String key = folder + "/" + UUID.randomUUID() + "_" + filename;

        // PDF 파일이면 "application/pdf", 아니면 "video/mp4"로 타입 지정
        String contentType = filename.toLowerCase().endsWith(".pdf") ? "application/pdf" : "video/mp4";

        URL presignedUrl = videoS3Service.generatePresignedUploadUrl(key, contentType, Duration.ofMinutes(10));

        Map<String, String> response = new HashMap<>();
        response.put("presignedUrl", presignedUrl.toString());
        response.put("fileKey", key);

        return ResponseEntity.ok(response);
    }
}
