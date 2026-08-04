package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.Baseline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BaselineRepository extends JpaRepository<Baseline, Long> {

    List<Baseline> findByMarketIdAndIsActiveTrue(Long marketId);

    /**
     * 2026-07-31 추가 (보고서 기능)
     * 해당 시장의 현재 유효한 현행안 1건.
     *
     * uq_simbsln01m_active_market(부분 유니크 인덱스)이 시장당 is_active=true를 1건으로
     * 강제하므로 결과는 0 또는 1건이다. 보고서는 비교 기준이 하나여야 성립하므로
     * 목록이 아니라 Optional로 받는다. 제약이 없는 환경을 대비해 최신 것이 잡히도록
     * baseline_id 역순으로 고정했다.
     */
    Optional<Baseline> findFirstByMarketIdAndIsActiveTrueOrderByBaselineIdDesc(Long marketId);
}
