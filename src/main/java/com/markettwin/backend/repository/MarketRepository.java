package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.Market;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MarketRepository extends JpaRepository<Market, Long> {

    // 상인회/지자체(비관리자) 사용자가 본인 담당 시장만 조회하도록
    // usrusrs01m.market_code와 동일한 comcode01m MKT 도메인 코드로 필터링.
    List<Market> findByMarketCode(String marketCode);
}
