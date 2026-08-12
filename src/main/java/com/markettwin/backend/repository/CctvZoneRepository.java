package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.CctvZone;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 2026-08-11 추가 (CCTV 관제 구역). 08-11 2차: 등록마다 행이 늘어 목록 페이징이 필요해졌다.
 * 시뮬레이션 구역을 다루는 ZoneRepository와는 별개의 테이블(mrkcctv01m)을 본다.
 */
public interface CctvZoneRepository extends JpaRepository<CctvZone, Long> {

    Page<CctvZone> findByMarketId(Long marketId, Pageable pageable);
}
