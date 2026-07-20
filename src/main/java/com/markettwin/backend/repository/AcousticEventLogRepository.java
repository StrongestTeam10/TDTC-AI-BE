package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.AcousticEventLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcousticEventLogRepository extends JpaRepository<AcousticEventLog, Long> {
}
