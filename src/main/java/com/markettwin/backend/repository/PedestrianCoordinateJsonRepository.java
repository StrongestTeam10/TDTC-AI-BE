package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.PedestrianCoordinateJson;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PedestrianCoordinateJsonRepository extends JpaRepository<PedestrianCoordinateJson, Long> {
    List<PedestrianCoordinateJson> findByClipIdAndFrameIdAndVideoId(
            Long clipId, Integer frameId, Long videoId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM PedestrianCoordinateJson p WHERE p.capturedAt < :threshold")
    void deleteByCapturedAtBefore(@org.springframework.data.repository.query.Param("threshold") java.time.Instant threshold);

}
