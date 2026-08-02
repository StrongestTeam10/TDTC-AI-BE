package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.BaselineResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BaselineResultRepository extends JpaRepository<BaselineResult, Long> {

    List<BaselineResult> findByBaselineId(Long baselineId);
}
