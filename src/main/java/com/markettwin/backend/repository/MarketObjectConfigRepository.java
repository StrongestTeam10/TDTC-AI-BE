package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.MarketObjectConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 2026-08-11 추가 (시장 오브젝트/구조 설정). 시장당 1행이라 marketId로 단건 조회한다.
 */
public interface MarketObjectConfigRepository extends JpaRepository<MarketObjectConfig, Long> {

    Optional<MarketObjectConfig> findByMarketId(Long marketId);
}
