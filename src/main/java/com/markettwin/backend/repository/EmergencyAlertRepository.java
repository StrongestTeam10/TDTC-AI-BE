package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.EmergencyAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EmergencyAlertRepository extends JpaRepository<EmergencyAlert, Long> {
    List<EmergencyAlert> findByIsResolvedFalse();

    boolean existsByZoneIdAndIsResolvedFalse(Long zoneId);

    Optional<EmergencyAlert> findFirstByZoneIdAndIsResolvedFalse(Long zoneId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM EmergencyAlert e WHERE e.alertedAt < :threshold")
    void deleteByAlertedAtBefore(@org.springframework.data.repository.query.Param("threshold") java.time.Instant threshold);

}
