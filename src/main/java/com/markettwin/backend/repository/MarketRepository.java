package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.Market;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketRepository extends JpaRepository<Market, Long> {
}
