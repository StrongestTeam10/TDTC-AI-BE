package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ZoneRepository extends JpaRepository<Zone, Long> {

    List<Zone> findByMarketId(Long marketId);
}
