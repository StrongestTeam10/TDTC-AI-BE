package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.ScenarioResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ScenarioResultRepository extends JpaRepository<ScenarioResult, Long> {
    List<ScenarioResult> findByScenarioId(Long scenarioId);

    List<ScenarioResult> findByScenarioIdIn(Collection<Long> scenarioIds);
}
