package com.markettwin.backend.controller;

import com.markettwin.backend.domain.entity.User;
import com.markettwin.backend.dto.response.ScenarioHistoryDto;
import com.markettwin.backend.security.CurrentUserProvider;
import com.markettwin.backend.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 2026-07-31 추가
 * 사용자가 실행한 시뮬레이션 이력 조회.
 *
 * 보고서 생성(/api/simulation/reports)과 경로를 나눈 이유: 이 목록은 보고서 유무와
 * 무관하게 실행 이력 전체를 보여주며, 보고서는 각 행에 붙는 부가 정보다.
 * 실행은 SimulationController가 담당하고 여기서는 조회만 하므로 별도 컨트롤러로 뒀다.
 */
@RestController
@RequestMapping("/api/simulation/scenarios")
@RequiredArgsConstructor
public class ScenarioHistoryController {

    private final ReportService reportService;
    private final CurrentUserProvider currentUserProvider;

    /** 로그인한 사용자가 실행한 시뮬레이션 이력을 최신순으로 반환한다. */
    @GetMapping("/my")
    public List<ScenarioHistoryDto> listMyScenarios() {
        User currentUser = currentUserProvider.getCurrentUser();
        return reportService.listMyScenarios(currentUser.getUserId());
    }
}
