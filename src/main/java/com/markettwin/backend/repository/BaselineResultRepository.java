package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.BaselineResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BaselineResultRepository extends JpaRepository<BaselineResult, Long> {

    List<BaselineResult> findByBaselineId(Long baselineId);

    /**
     * 2026-07-31 추가 (보고서 기능)
     * 이 현행안의 가장 최근 결과 1건.
     *
     * 보고서는 "시나리오 1건 vs 그 시장의 현행안"이므로 인구수로 좁히지 않는다.
     * 인구수가 서로 다를 수 있다는 사실은 SIM이 보고서에 시나리오별 투입 인구를
     * 표시하고 분석 가정에 명시하는 방식으로 다룬다.
     *
     * 정렬 기준(executed_at → id)은 ReportService.latestResultOf() 및
     * SIM report_adapter의 _latest_result_by_scenario()와 동일하게 맞췄다.
     * 한쪽만 바꾸면 기준안과 대안이 서로 다른 규칙으로 뽑혀 결과가 어긋난다.
     */
    Optional<BaselineResult>
    findFirstByBaselineIdOrderByExecutedAtDescBaselineResultIdDesc(Long baselineId);
}
