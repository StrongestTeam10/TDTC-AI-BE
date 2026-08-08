package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.EmergencyAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EmergencyAlertRepository extends JpaRepository<EmergencyAlert, Long> {
    List<EmergencyAlert> findByIsResolvedFalse();

    boolean existsByZoneIdAndIsResolvedFalse(Long zoneId);

    Optional<EmergencyAlert> findFirstByZoneIdAndIsResolvedFalse(Long zoneId);
}
