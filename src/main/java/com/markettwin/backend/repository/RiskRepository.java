package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.Risk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface RiskRepository extends JpaRepository<Risk, Long> {

    List<Risk> findByDetectedAtLessThanEqualOrderByDetectedAtDesc(Instant snapshotTime);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM Risk r WHERE r.detectedAt < :threshold")
    void deleteByDetectedAtBefore(@org.springframework.data.repository.query.Param("threshold") java.time.Instant threshold);

    @Query(value = "SELECT r.* FROM mrkrisk01m r " +
            "JOIN pedaggr01h p ON r.coord_id = p.coord_id " +
            "WHERE p.zone_id = :zoneId " +
            "ORDER BY r.detected_at DESC LIMIT 1",
            nativeQuery = true)
    java.util.Optional<com.markettwin.backend.domain.entity.Risk> findLatestRiskByZoneId(@Param("zoneId") Long zoneId);

    // 추가: 부모(clipId) 리스트를 기반으로 손자(Risk) 일괄 삭제
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM Risk r WHERE r.coordId IN (SELECT p.coordId FROM PedestrianCoordinateJson p WHERE p.clipId IN :clipIds)")
    void deleteByVideoClipIds(@org.springframework.data.repository.query.Param("clipIds") java.util.List<Long> clipIds);


}
