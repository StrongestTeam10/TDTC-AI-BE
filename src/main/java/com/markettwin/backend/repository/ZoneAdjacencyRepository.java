package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.ZoneAdjacency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ZoneAdjacencyRepository extends JpaRepository<ZoneAdjacency, Long> {

    // mrkadjc01m에서 market_id 컬럼이 빠지면서(from_zone_id가 속한
    // 구역을 통해 이미 알 수 있는 값이라 중복이었음), 시장별 조회는 Zone과 조인해서
    // 구현함. from_zone_id 기준으로만 조인하는 이유: uq_mrkadjc01m_edge 제약상
    // from/to 조합이 항상 같은 시장 내에서만 만들어진다는 전제(양방향은 두 행으로
    // 따로 저장되므로 to_zone_id 쪽도 결국 같은 시장의 from_zone_id로 한 번 더 잡힘).
    @Query("""
            SELECT za FROM ZoneAdjacency za
            WHERE za.fromZoneId IN (
                SELECT z.zoneId FROM Zone z WHERE z.marketId = :marketId
            )
            """)
    List<ZoneAdjacency> findByMarketId(@Param("marketId") Long marketId);

    /** 통행 가능한 연결만 조회 (시뮬레이션 그래프 구성용) */
    @Query("""
            SELECT za FROM ZoneAdjacency za
            WHERE za.isActive = true
              AND za.fromZoneId IN (
                SELECT z.zoneId FROM Zone z WHERE z.marketId = :marketId
              )
            """)
    List<ZoneAdjacency> findByMarketIdAndIsActiveTrue(@Param("marketId") Long marketId);

    List<ZoneAdjacency> findByFromZoneId(Long fromZoneId);

    // (구역 삭제): 통로는 구역에서 파생된 값이라, 구역이 없어지면
    // 그 구역이 양 끝 중 어느 쪽이든 걸려 있는 행을 함께 지운다.
    void deleteByFromZoneIdOrToZoneId(Long fromZoneId, Long toZoneId);
}
