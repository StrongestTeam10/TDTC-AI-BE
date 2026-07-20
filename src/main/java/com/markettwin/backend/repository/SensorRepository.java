package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SensorRepository extends JpaRepository<Sensor, Long> {
    List<Sensor> findByZoneId(Long zoneId);
}
