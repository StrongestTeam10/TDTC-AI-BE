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

    private final VideoS3Service videoS3Service; // 여기를 사용자님 전용 서비스로 변경

    @GetMapping("/presigned-url")
    public ResponseEntity<Map<String, String>> getPresignedUrl(@RequestParam String filename) {
        String key = "videos/" + UUID.randomUUID() + "_" + filename;

        URL presignedUrl = videoS3Service.generatePresignedUploadUrl(key, "video/mp4", Duration.ofMinutes(10));

        Map<String, String> response = new HashMap<>();
        response.put("presignedUrl", presignedUrl.toString());
        response.put("fileKey", key);

        return ResponseEntity.ok(response);
    }
}
