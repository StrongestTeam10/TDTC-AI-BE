package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.Building;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface BuildingRepository extends JpaRepository<Building, Long> {

    List<Building> findByMarketId(Long marketId);

    // 2026-08-14 추가 (건물 자동 적재): 이미 건물이 있는 시장에 덮어쓰는 것을 막기 위한 확인.
    long countByMarketId(Long marketId);

    // overwrite를 명시했을 때만 호출한다.
    void deleteByMarketId(Long marketId);

    /**
     * 2026-08-14 추가: 이미 저장된 건물관리번호를 걸러내기 위한 조회.
     *
     * pnu_code에는 시장과 무관한 전체 UNIQUE 제약(mrkbldg01m_pnu_code_key)이 걸려 있다.
     * 이웃한 두 시장의 반경이 겹치면 같은 건물이 이미 들어와 있을 수 있어서, 넣기 전에
     * 걸러내지 않으면 적재 전체가 제약 위반으로 실패한다.
     */
    @Query("SELECT b.pnuCode FROM Building b WHERE b.pnuCode IN :pnuCodes")
    List<String> findExistingPnuCodes(@Param("pnuCodes") Collection<String> pnuCodes);
}