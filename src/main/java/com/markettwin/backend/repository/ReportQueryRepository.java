package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.ScenarioResult;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * 시나리오 실행 이력 조회 전용 읽기 리포지토리.
 *
 * 시나리오(simscnr01m)와 결과(simrslt01d)를 조인해 목록용 형태로 뽑는다.
 * 네이티브 쿼리를 쓰는 이유는 두 가지다.
 *   1) 목록에 필요한 컬럼만 뽑아 엔티티 전체를 적재하지 않는다.
 *   2) simscnr01m.user_id 는 Scenario 엔티티에 매핑돼 있지 않다. 이 컬럼은 시나리오
 *      저장을 담당하는 쪽에서 채우며, 엔티티 매핑이 추가되기 전에도 조회가 가능해야 한다.
 *
 * 보고서 유무로 거르지 않는다. 사용자가 보려는 것은 "내가 실행한 시뮬레이션 이력"이고,
 * 보고서는 그중 일부에만 붙어 있는 부가 정보다. generated_report_path가 채워진 행이
 * 곧 보고서가 있는 실행이므로, 별도 보고서 테이블 없이 둘 다 한 번에 나온다.
 */
public interface ReportQueryRepository extends Repository<ScenarioResult, Long> {

    /** 목록 한 줄. 네이티브 쿼리의 컬럼 별칭과 getter 이름이 일치해야 매핑된다. */
    interface ReportRow {
        Long getScenarioId();
        String getScenarioName();
        Long getMarketId();
        String getMarketName();
        Integer getAgentCount();
        String getPolicyTypeCode();
        /** 시나리오 등록 시각(UTC). 표시용 시나리오명 조립에 쓴다. */
        Instant getRegDatetime();
        Instant getExecutedAt();
        /** 문서 표지에 실제로 박힌 제목. 보고서 기능 도입 전 데이터는 null. */
        String getReportTitle();
        String getStorageKey();
    }

    @Query(value = """
            SELECT s.scenario_id          AS scenarioId,
                   s.scenario_name        AS scenarioName,
                   s.market_id            AS marketId,
                   m.market_name          AS marketName,
                   s.agent_count          AS agentCount,
                   s.policy_type_code     AS policyTypeCode,
                   s.reg_datetime         AS regDatetime,
                   r.executed_at          AS executedAt,
                   r.report_title         AS reportTitle,
                   r.generated_report_path AS storageKey
              FROM simrslt01d r
              JOIN simscnr01m s ON s.scenario_id = r.scenario_id
              LEFT JOIN mrkaddr01m m ON m.market_id = s.market_id
             WHERE s.user_id = :userId
             ORDER BY r.executed_at DESC, r.result_id DESC
            """, nativeQuery = true)
    List<ReportRow> findScenarioHistoryByUserId(@Param("userId") Long userId);

    /**
     * 보고서 저장 결과(S3 키와 문서 제목)를 결과 행에 기록한다.
     *
     * ScenarioResult 엔티티에는 report_title 매핑이 없어 네이티브 UPDATE로 처리한다.
     * 이 컬럼들은 보고서 기능이 소유하므로, 시뮬레이션 저장을 담당하는 엔티티를
     * 건드리지 않고 여기서만 다루는 편이 경계가 분명하다.
     */
    @Modifying
    @Transactional
    @Query(value = """
            UPDATE simrslt01d
               SET generated_report_path = :storageKey,
                   report_title = :reportTitle
             WHERE result_id = :resultId
            """, nativeQuery = true)
    void updateReportInfo(@Param("resultId") Long resultId,
                         @Param("storageKey") String storageKey,
                         @Param("reportTitle") String reportTitle);
}
