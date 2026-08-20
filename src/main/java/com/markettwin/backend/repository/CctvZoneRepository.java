package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.CctvZone;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * (CCTV 관제 구역). 2차: 등록마다 행이 늘어 목록 페이징이 필요해졌다.
 * 시뮬레이션 구역을 다루는 ZoneRepository와는 별개의 테이블(mrkcctv01m)을 본다.
 */
public interface CctvZoneRepository extends JpaRepository<CctvZone, Long> {

    Page<CctvZone> findByMarketId(Long marketId, Pageable pageable);

    /** 추가(관측 초기배치): 그 시뮬 구역의 활성 CCTV 구역 4점 폴리곤. */
    Optional<CctvZone> findFirstByZoneIdAndIsActiveTrue(Long zoneId);

    // (구역 삭제): 시뮬레이션 구역을 지우기 전에, 그 구역에 걸린
    // CCTV가 있는지 확인해 FK 위반 대신 안내 메시지를 주기 위함.
    long countByZoneId(Long zoneId);
}
