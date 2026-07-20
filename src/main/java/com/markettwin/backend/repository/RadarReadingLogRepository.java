package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.RadarReadingLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RadarReadingLogRepository extends JpaRepository<RadarReadingLog, Long> {
}
