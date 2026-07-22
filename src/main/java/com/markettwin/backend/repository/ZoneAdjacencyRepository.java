package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.ZoneAdjacency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ZoneAdjacencyRepository extends JpaRepository<ZoneAdjacency, Long> {

    List<ZoneAdjacency> findByMarketId(Long marketId);

    /** 통행 가능한 연결만 조회 (시뮬레이션 그래프 구성용) */
    List<ZoneAdjacency> findByMarketIdAndIsActiveTrue(Long marketId);

    List<ZoneAdjacency> findByFromZoneId(Long fromZoneId);
}
