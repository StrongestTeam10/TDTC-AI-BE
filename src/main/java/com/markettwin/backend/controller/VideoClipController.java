package com.markettwin.backend.controller;

import com.markettwin.backend.domain.entity.VideoClip;
import com.markettwin.backend.dto.request.VideoClipWebhookRequest;
import com.markettwin.backend.dto.response.VideoClipDto;
import com.markettwin.backend.repository.VideoClipRepository;
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
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/video-clips")
@RequiredArgsConstructor
public class VideoClipController {

    private static final Logger log = LoggerFactory.getLogger(VideoClipController.class);

    private final VideoClipRepository repository;
    private final VideoS3Service videoS3Service;

    @Value("${ai.secret-key}")
    private String aiSecretKey;

    @GetMapping
    public List<VideoClipDto> getAllVideoClips(@RequestParam(required = false) Long zoneId) {
        // ★ 추가됨: zoneId가 파라미터로 넘어오면 필터링, 없으면 전체 조회
        List<VideoClip> clips = (zoneId != null)
                ? repository.findByZoneId(zoneId)
                : repository.findAll();

        return clips.stream()
                .filter(clip -> !Boolean.TRUE.equals(clip.getIsDeleted())) // 삭제된 영상은 필터링(프론트엔드 노출 금지)
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

        if (!ApiKeys.matches(apiKey, aiSecretKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "❌ 경고: API 키가 일치하지 않거나 누락되었습니다."));
        }

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

        // S3 URL은 남기지 않는다 - 서명 없는 객체 경로라도 버킷 구조가 로그로 새어
        // 나가고, 외부(AI 서버)에서 온 문자열이라 그대로 찍으면 로그 위조에 쓰인다.
        // 어느 클립이 들어왔는지 추적하는 데는 식별자로 충분하다.
        log.info("AI 긴급 클립 수신: clipId={}, zoneId={}, clipType={}",
                savedClip.getClipId(), savedClip.getZoneId(), savedClip.getClipType());

        return ResponseEntity.ok().body(Map.of(
                "clipId", savedClip.getClipId(),
                "message", "비디오 클립 DB 등록 완료"
        ));
    }
}
