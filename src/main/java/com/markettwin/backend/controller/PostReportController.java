package com.markettwin.backend.controller;
import com.markettwin.backend.dto.response.PostReportDto;
import com.markettwin.backend.repository.PostReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/post-reports")
@RequiredArgsConstructor
public class PostReportController {
    private final PostReportRepository repository;

    @GetMapping
    public List<PostReportDto> getAllPostReports() {
        return repository.findAll().stream().map(PostReportDto::from).toList();
    }
}
