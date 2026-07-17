package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.CctvDetection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface CctvDetectionRepository extends JpaRepository<CctvDetection, Long> {

    List<CctvDetection> findByTimestamp(Instant timestamp);

    List<CctvDetection> findByTimestampBetween(Instant start, Instant end);

    // 최신 시점 자동 조회용
    List<CctvDetection> findTop100ByOrderByTimestampDesc();
}
