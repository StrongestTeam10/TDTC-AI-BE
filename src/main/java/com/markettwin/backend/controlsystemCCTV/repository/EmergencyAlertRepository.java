package com.markettwin.backend.controlsystemCCTV.repository;

import com.markettwin.backend.controlsystemCCTV.entity.EmergencyAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EmergencyAlertRepository extends JpaRepository<EmergencyAlert, Long> {
    List<EmergencyAlert> findByIsResolvedFalse();
}
