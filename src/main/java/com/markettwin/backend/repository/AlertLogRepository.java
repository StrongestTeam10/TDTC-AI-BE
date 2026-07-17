package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.AlertLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface AlertLogRepository extends JpaRepository<AlertLog, Long> {

    List<AlertLog> findByTimestampLessThanEqualOrderByTimestampDesc(Instant snapshotTime);
}
