package com.markettwin.backend.controlsystemCCTV.repository;

import com.markettwin.backend.controlsystemCCTV.entity.PedestrianCoordinate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PedestrianCoordinateRepository extends JpaRepository<PedestrianCoordinate, Long> {
    List<PedestrianCoordinate> findByZoneIdAndFrameIdAndAnalysisModeAndVideoId(
            Long zoneId, Integer frameId, String analysisMode, Long videoId);
}
