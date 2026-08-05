package com.markettwin.backend.controller;
import com.markettwin.backend.dto.response.VideoClipDto;
import com.markettwin.backend.repository.VideoClipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/video-clips")
@RequiredArgsConstructor
public class VideoClipController {
    private final VideoClipRepository repository;

    @GetMapping
    public List<VideoClipDto> getAllVideoClips() {
        return repository.findAll().stream().map(VideoClipDto::from).toList();
    }
}
