package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.LidarReadingLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LidarReadingLogRepository extends JpaRepository<LidarReadingLog, Long> {
}
