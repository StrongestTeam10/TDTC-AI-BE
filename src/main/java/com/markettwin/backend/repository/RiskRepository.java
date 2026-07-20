package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.Risk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface RiskRepository extends JpaRepository<Risk, Long> {

    List<Risk> findByDetectedAtLessThanEqualOrderByDetectedAtDesc(Instant snapshotTime);
}
