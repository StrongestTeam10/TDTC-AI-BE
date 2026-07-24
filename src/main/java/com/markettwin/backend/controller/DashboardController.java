package com.markettwin.backend.controller;

import com.markettwin.backend.dto.response.DashboardSnapshotDto;
import com.markettwin.backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/snapshot")
    public DashboardSnapshotDto getSnapshot(
            @RequestParam Long marketId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant capturedAt,
            @RequestParam(required = false, defaultValue = "false") Boolean persistRisk,
            @RequestParam(required = false, defaultValue = "true") Boolean includeAgents
    ) {
        return dashboardService.getSnapshot(marketId, capturedAt, persistRisk, includeAgents);
    }

    @GetMapping("/timestamps")
    public List<Instant> getAvailableTimestamps() {
        return dashboardService.getAvailableTimestamps();
    }
}
