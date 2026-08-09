package com.markettwin.backend.controller;

import com.markettwin.backend.domain.entity.User;
import com.markettwin.backend.dto.response.PageResponseDto;
import com.markettwin.backend.dto.response.ScenarioDetailDto;
import com.markettwin.backend.dto.response.ScenarioHistoryDto;
import com.markettwin.backend.security.CurrentUserProvider;
import com.markettwin.backend.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    public PageResponseDto<ScenarioHistoryDto> listMyScenarios(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "all") String searchField,
            @RequestParam(defaultValue = "false") boolean withReportOnly,
            @RequestParam(required = false) Integer minRiskScore,
            @RequestParam(required = false) Integer maxRiskScore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User currentUser = currentUserProvider.getCurrentUser();
        return reportService.listMyScenarios(
                currentUser.getUserId(), keyword, searchField, withReportOnly,
                minRiskScore, maxRiskScore, page, size);
    }

    /**
     * 2026-08-08 추가
     * 시나리오 한 건의 실행 설정(오브젝트·이벤트·통로정책·닫은 게이트)을 반환한다.
     * 목록에서 행을 펼쳤을 때 쓴다.
     *
     * 목록의 시나리오명은 같은 시장·같은 정책유형이면 서로 구분되지 않아, 어느 실행인지
     * 알려면 설정을 봐야 한다. 소유자 검증은 보고서와 같은 기준이며 관리자는 우회한다.
     */
    @GetMapping("/{scenarioId}")
    public ScenarioDetailDto getScenarioDetail(@PathVariable Long scenarioId) {
        return reportService.getScenarioDetail(scenarioId);
    }

    /**
     * 2026-08-06 추가
     * 실행자와 무관하게 시뮬레이션 이력 전체를 최신순으로 반환한다. 관리자(ROL01) 전용이며,
     * 관제요원이 호출하면 ReportService가 403으로 거절한다(SecurityConfig의 경로 규칙은
     * ROL02까지 열려 있어 경로만으로는 좁혀지지 않는다).
     *
     * marketId를 주면 그 시장만 거른다. 생략하면 전체 시장이다.
     *
     * searchField는 keyword를 어느 항목에서 찾을지 정한다
     * (all/market/policy/reportTitle/owner). owner는 전체 조회에서만 의미가 있다.
     * 시나리오명은 대상에서 빼뒀다 - DB에 저장된 원본과 화면에 보이는 표시명이 달라
     * 보이는 대로 입력하면 걸리지 않는다(ScenarioDisplayNameResolver 참고).
     *
     * minRiskScore/maxRiskScore는 위험 점수(0~100) 범위다. 등급 경계는 화면이 정하고
     * 여기서는 숫자 범위만 받는다. 둘 다 생략하면 위험도로 거르지 않으며, 이때만
     * 점수가 없는(NULL) 옛 이력도 함께 나온다.
     */
    @GetMapping
    public PageResponseDto<ScenarioHistoryDto> listAllScenarios(
            @RequestParam(required = false) Long marketId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "all") String searchField,
            @RequestParam(defaultValue = "false") boolean withReportOnly,
            @RequestParam(required = false) Integer minRiskScore,
            @RequestParam(required = false) Integer maxRiskScore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return reportService.listAllScenarios(
                marketId, keyword, searchField, withReportOnly,
                minRiskScore, maxRiskScore, page, size);
    }
}
