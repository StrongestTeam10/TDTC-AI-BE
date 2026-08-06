package com.markettwin.backend.controller;
import com.markettwin.backend.dto.response.VideoClipDto;
import com.markettwin.backend.repository.VideoClipRepository;
import com.markettwin.backend.service.VideoS3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/v1/video-clips")
@RequiredArgsConstructor
public class VideoClipController {

    private final VideoClipRepository repository;
    private final VideoS3Service videoS3Service;

    @GetMapping
    public List<VideoClipDto> getAllVideoClips() {
        return repository.findAll().stream().map(clip -> {
            String viewUrl = (clip.getS3ClipUrl() != null && !clip.getS3ClipUrl().isBlank())
                    ? videoS3Service.generatePresignedDownloadUrl(clip.getS3ClipUrl(), Duration.ofHours(1)).toString()
                    : null;
            return VideoClipDto.from(clip, viewUrl);
        }).toList();
    }
}
