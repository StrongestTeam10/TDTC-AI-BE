package com.markettwin.backend.controller;

import com.markettwin.backend.domain.entity.VideoClip;
import com.markettwin.backend.dto.request.VideoClipWebhookRequest;
import com.markettwin.backend.dto.response.VideoClipDto;
import com.markettwin.backend.repository.VideoClipRepository;
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
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/video-clips")
@RequiredArgsConstructor
public class VideoClipController {

    private final VideoClipRepository repository;
    private final VideoS3Service videoS3Service;

    @Value("${ai.secret-key}")
    private String aiSecretKey;

    @GetMapping
    public List<VideoClipDto> getAllVideoClips() {
        return repository.findAll().stream()
                .filter(clip -> !Boolean.TRUE.equals(clip.getIsDeleted())) // ★ 추가: 삭제된 영상은 필터링(프론트엔드 노출 금지)
                .map(clip -> {
                    String viewUrl = (clip.getS3ClipUrl() != null && !clip.getS3ClipUrl().isBlank())
                            ? videoS3Service.generatePresignedDownloadUrl(clip.getS3ClipUrl(), Duration.ofHours(1)).toString()
                            : null;
                    return VideoClipDto.from(clip, viewUrl);
                }).toList();
    }

    @PostMapping
    public ResponseEntity<?> receiveVideoClipWebhook(
            @RequestHeader(value = "X-API-KEY", required = false) String apiKey,
            @RequestBody VideoClipWebhookRequest payload) {

        if (apiKey == null || !apiKey.equals(aiSecretKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "❌ 경고: API 키가 일치하지 않거나 누락되었습니다."));
        }

        System.out.println("✅ 파이썬으로부터 긴급 클립 URL 수신 완료: " + payload.getS3ClipUrl());

        VideoClip clip = VideoClip.builder()
                .zoneId(payload.getZoneId())
                .clipType(payload.getClipType())
                .s3ClipUrl(payload.getS3ClipUrl())
                .startTime(Instant.parse(payload.getStartTime()))
                .endTime(Instant.parse(payload.getEndTime()))
                .expiresAt(Instant.now().plus(Duration.ofDays(30)))
                .isDownloaded(false)
                .isDeleted(false)
                .build();

        VideoClip savedClip = repository.save(clip);

        return ResponseEntity.ok().body(Map.of(
                "clipId", savedClip.getClipId(),
                "message", "비디오 클립 DB 등록 완료"
        ));
    }
}
