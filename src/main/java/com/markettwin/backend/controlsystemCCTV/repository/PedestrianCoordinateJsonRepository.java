package com.markettwin.backend.controlsystemCCTV.repository;

import com.markettwin.backend.controlsystemCCTV.entity.PedestrianCoordinateJson;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PedestrianCoordinateJsonRepository extends JpaRepository<PedestrianCoordinateJson, Long> {
    List<PedestrianCoordinateJson> findByZoneIdAndFrameIdAndAnalysisModeAndVideoId(
            Long zoneId, Integer frameId, String analysisMode, Long videoId);
}
