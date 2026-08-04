package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.EmergencyAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EmergencyAlertRepository extends JpaRepository<EmergencyAlert, Long> {
    List<EmergencyAlert> findByIsResolvedFalse();

    boolean existsByZoneIdAndIsResolvedFalse(Long zoneId);
}
