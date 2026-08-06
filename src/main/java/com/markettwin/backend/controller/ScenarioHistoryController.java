package com.markettwin.backend.controller;

import com.markettwin.backend.domain.entity.User;
import com.markettwin.backend.dto.response.ScenarioHistoryDto;
import com.markettwin.backend.security.CurrentUserProvider;
import com.markettwin.backend.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    /**
     * 2026-08-06 추가
     * 실행자와 무관하게 시뮬레이션 이력 전체를 최신순으로 반환한다. 관리자(ROL01) 전용이며,
     * 관제요원이 호출하면 ReportService가 403으로 거절한다(SecurityConfig의 경로 규칙은
     * ROL02까지 열려 있어 경로만으로는 좁혀지지 않는다).
     *
     * marketId를 주면 그 시장만 거른다. 생략하면 전체 시장이다.
     */
    @GetMapping
    public List<ScenarioHistoryDto> listAllScenarios(
            @RequestParam(required = false) Long marketId) {
        return reportService.listAllScenarios(marketId);
    }
}
