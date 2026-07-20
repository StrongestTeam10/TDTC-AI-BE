package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.RadarReading;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RadarReadingRepository extends JpaRepository<RadarReading, Long> {
    List<RadarReading> findBySensorIdOrderByUpdatedAtDesc(Long sensorId);
}
