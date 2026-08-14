package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.Risk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface RiskRepository extends JpaRepository<Risk, Long> {

    List<Risk> findByDetectedAtLessThanEqualOrderByDetectedAtDesc(Instant snapshotTime);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM Risk r WHERE r.detectedAt < :threshold")
    void deleteByDetectedAtBefore(@org.springframework.data.repository.query.Param("threshold") java.time.Instant threshold);

}
