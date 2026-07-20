package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.EntityChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntityChangeLogRepository extends JpaRepository<EntityChangeLog, Long> {
}
