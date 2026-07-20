package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.LidarReading;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LidarReadingRepository extends JpaRepository<LidarReading, Long> {
    List<LidarReading> findBySensorIdOrderByUpdatedAtDesc(Long sensorId);
}
