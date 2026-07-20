package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.AcousticEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface AcousticEventRepository extends JpaRepository<AcousticEvent, Long> {
    List<AcousticEvent> findByDetectedAtAfterOrderByDetectedAtDesc(Instant since);
}
