package com.markettwin.backend.controller;

import com.markettwin.backend.dto.response.SpatialNodeDto;
import com.markettwin.backend.service.SpatialService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/spatial")
@RequiredArgsConstructor
public class SpatialController {

    private final SpatialService spatialService;

    @GetMapping("/layout")
    public List<SpatialNodeDto> getLayout() {
        return spatialService.getLayout();
    }
}
