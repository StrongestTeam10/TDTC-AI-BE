package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.PedestrianCoordinateJson;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PedestrianCoordinateJsonRepository extends JpaRepository<PedestrianCoordinateJson, Long> {
    List<PedestrianCoordinateJson> findByClipIdAndFrameIdAndVideoId(
            Long clipId, Integer frameId, Integer videoId);
}
