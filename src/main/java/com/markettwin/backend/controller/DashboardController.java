package com.markettwin.backend.controller;

import com.markettwin.backend.domain.entity.User;
import com.markettwin.backend.dto.response.DashboardSnapshotDto;
import com.markettwin.backend.security.CurrentUserProvider;
import com.markettwin.backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * 2026-07-27 변경: 시장/구역별 권한 분리 적용을 위해 CurrentUserProvider로 로그인
 * 사용자를 조회해 DashboardService에 전달함(본인 담당 시장이 아닌 marketId로 요청하면
 * 403 응답).
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/snapshot")
    public DashboardSnapshotDto getSnapshot(
            @RequestParam Long marketId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant capturedAt,
            @RequestParam(required = false, defaultValue = "false") Boolean persistRisk,
            @RequestParam(required = false, defaultValue = "true") Boolean includeAgents
    ) {
        User currentUser = currentUserProvider.getCurrentUser();
        return dashboardService.getSnapshot(marketId, capturedAt, persistRisk, includeAgents, currentUser);
    }

    @GetMapping("/timestamps")
    public List<Instant> getAvailableTimestamps() {
        return dashboardService.getAvailableTimestamps();
    }
}
