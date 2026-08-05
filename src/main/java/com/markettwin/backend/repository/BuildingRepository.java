package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.Building;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BuildingRepository extends JpaRepository<Building, Long> {

    List<Building> findByMarketId(Long marketId);
}