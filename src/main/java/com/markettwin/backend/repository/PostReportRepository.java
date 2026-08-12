package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.PostReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PostReportRepository extends JpaRepository<PostReport, Long> {
    List<PostReport> findByTargetDateAndVideoId(LocalDate targetDate, Long videoId);

    // ★ 추가됨: 긴급알람(emgalrt01h)과 조인해서 해당 구역(zone_id)의 명세서만 가져오는 네이티브 쿼리
    @Query(value = "SELECT p.* FROM pstrprt01h p JOIN emgalrt01h e ON p.alert_id = e.alert_id WHERE e.zone_id = :zoneId", nativeQuery = true)
    List<PostReport> findByZoneIdNative(@Param("zoneId") Long zoneId);
}
