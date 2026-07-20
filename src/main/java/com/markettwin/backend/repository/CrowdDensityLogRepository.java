package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.CrowdDensityLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CrowdDensityLogRepository extends JpaRepository<CrowdDensityLog, Long> {
    List<CrowdDensityLog> findByCrowdDensityId(Long crowdDensityId);
}
