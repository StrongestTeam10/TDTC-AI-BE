package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.CrowdDensity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface CrowdDensityRepository extends JpaRepository<CrowdDensity, Long> {

    List<CrowdDensity> findByCapturedAt(Instant capturedAt);

    List<CrowdDensity> findTop100ByOrderByCapturedAtDesc();

    List<CrowdDensity> findByMarketIdOrderByCapturedAtDesc(Long marketId);
}
