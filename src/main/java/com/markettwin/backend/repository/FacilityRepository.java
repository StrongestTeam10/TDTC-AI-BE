package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.Facility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FacilityRepository extends JpaRepository<Facility, Long> {
    List<Facility> findByMarketId(Long marketId);
}
