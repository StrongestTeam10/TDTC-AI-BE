package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.Scenario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScenarioRepository extends JpaRepository<Scenario, Long> {
}
