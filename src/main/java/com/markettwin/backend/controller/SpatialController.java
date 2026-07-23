package com.markettwin.backend.controller;

import com.markettwin.backend.dto.response.SpatialNodeDto;
import com.markettwin.backend.service.SpatialLayoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SpatialController {

    private final SpatialLayoutService spatialLayoutService;

    /**
     * 프론트 axios client가 호출하는 GET /spatial/layout
     * (baseURL이 /api라 실제 경로는 /api/spatial/layout).
     */
    @GetMapping("/api/spatial/layout")
    public List<SpatialNodeDto> getLayout(
            @RequestParam(defaultValue = "1") Long marketId
    ) {
        return spatialLayoutService.getLayout(marketId);
    }
}