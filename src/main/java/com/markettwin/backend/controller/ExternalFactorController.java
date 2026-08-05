package com.markettwin.backend.controller;
import com.markettwin.backend.dto.response.ExternalFactorDto;
import com.markettwin.backend.repository.ExternalFactorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/external-factors")
@RequiredArgsConstructor
public class ExternalFactorController {
    private final ExternalFactorRepository repository;

    @GetMapping
    public List<ExternalFactorDto> getAllExternalFactors() {
        return repository.findAll().stream().map(ExternalFactorDto::from).toList();
    }
}
