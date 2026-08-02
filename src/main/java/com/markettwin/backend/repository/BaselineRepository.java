package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.Baseline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BaselineRepository extends JpaRepository<Baseline, Long> {

    List<Baseline> findByMarketIdAndIsActiveTrue(Long marketId);
}
