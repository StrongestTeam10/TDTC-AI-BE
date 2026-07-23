package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.CrowdDensity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface CrowdDensityRepository extends JpaRepository<CrowdDensity, Long> {

    List<CrowdDensity> findByCapturedAt(Instant capturedAt);

    List<CrowdDensity> findTop100ByOrderByCapturedAtDesc();

    /**
     * 시장 단위 조회.
     * CRDDNST01M에는 market_id가 없으므로 MRKADDR01D(구역)를 조인해서 필터링한다.
     */
    @Query("""
           SELECT c FROM CrowdDensity c, Zone z
           WHERE z.zoneId = c.zoneId AND z.marketId = :marketId
           ORDER BY c.capturedAt DESC
           """)
    List<CrowdDensity> findByMarketId(@Param("marketId") Long marketId);
}
