package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.PostReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface PostReportRepository extends JpaRepository<PostReport, Long> {
    List<PostReport> findByTargetDateAndVideoId(LocalDate targetDate, Integer videoId);
}
