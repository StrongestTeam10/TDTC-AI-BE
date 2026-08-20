package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.ScenarioResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
        /**
         * 실행자 이름. user_id가 NULL인 옛 데이터는 null이다.
         *
         * 관리자 전체 목록에서 "누가 돌린 실행인지"를 구분하는 유일한 단서다.
         * 본인 목록에서는 항상 자기 이름이라 화면에 쓰이지 않는다.
         */
        String getOwnerName();
        /**
         * 이 실행의 종합 위험 점수(simrslt01d.predicted_risk_score, 0~100).
         *
         * 보고서 기능 도입 전 데이터와 위험도 계산이 실패한 실행은 null이다.
         * 등급(안전/주의/위험/심각) 구분은 화면이 맡고 여기서는 점수만 돌려준다 -
         * 등급 경계를 바꿀 때 BE를 함께 고쳐야 하는 상황을 만들지 않기 위함이다.
         */
        Integer getPredictedRiskScore();
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
                   r.generated_report_path AS storageKey,
                   u.name                 AS ownerName,
                   r.predicted_risk_score AS predictedRiskScore
              FROM simrslt01d r
              JOIN simscnr01m s ON s.scenario_id = r.scenario_id
              LEFT JOIN comcode01m c
                     ON c.code_cob = 'POL' AND c.code = s.policy_type_code
             LEFT JOIN mrkaddr01m m ON m.market_id = s.market_id
              LEFT JOIN usrusrs01m u ON u.user_id = s.user_id
             WHERE s.user_id = :userId
               AND (CAST(:withReportOnly AS boolean) = FALSE
                    OR NULLIF(BTRIM(r.generated_report_path), '') IS NOT NULL)
               AND (CAST(:minRiskScore AS int) IS NULL
                    OR r.predicted_risk_score >= CAST(:minRiskScore AS int))
               AND (CAST(:maxRiskScore AS int) IS NULL
                    OR r.predicted_risk_score <= CAST(:maxRiskScore AS int))
               AND (CAST(:keyword AS text) IS NULL
                    OR (CAST(:searchField AS text) IN ('all', 'market')
                        AND m.market_name ILIKE '%' || CAST(:keyword AS text) || '%')
                    OR (CAST(:searchField AS text) IN ('all', 'policy')
                        AND c.code_name ILIKE '%' || CAST(:keyword AS text) || '%')
                    OR (CAST(:searchField AS text) IN ('all', 'reportTitle')
                        AND r.report_title ILIKE '%' || CAST(:keyword AS text) || '%'))
             ORDER BY r.executed_at DESC, r.result_id DESC
            """, countQuery = """
            SELECT COUNT(*)
              FROM simrslt01d r
              JOIN simscnr01m s ON s.scenario_id = r.scenario_id
              LEFT JOIN comcode01m c
                     ON c.code_cob = 'POL' AND c.code = s.policy_type_code
              LEFT JOIN mrkaddr01m m ON m.market_id = s.market_id
             WHERE s.user_id = :userId
               AND (CAST(:withReportOnly AS boolean) = FALSE
                    OR NULLIF(BTRIM(r.generated_report_path), '') IS NOT NULL)
               AND (CAST(:minRiskScore AS int) IS NULL
                    OR r.predicted_risk_score >= CAST(:minRiskScore AS int))
               AND (CAST(:maxRiskScore AS int) IS NULL
                    OR r.predicted_risk_score <= CAST(:maxRiskScore AS int))
               AND (CAST(:keyword AS text) IS NULL
                    OR (CAST(:searchField AS text) IN ('all', 'market')
                        AND m.market_name ILIKE '%' || CAST(:keyword AS text) || '%')
                    OR (CAST(:searchField AS text) IN ('all', 'policy')
                        AND c.code_name ILIKE '%' || CAST(:keyword AS text) || '%')
                    OR (CAST(:searchField AS text) IN ('all', 'reportTitle')
                        AND r.report_title ILIKE '%' || CAST(:keyword AS text) || '%'))
            """, nativeQuery = true)
    Page<ReportRow> findScenarioHistoryByUserId(
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            @Param("searchField") String searchField,
            @Param("withReportOnly") boolean withReportOnly,
            @Param("minRiskScore") Integer minRiskScore,
            @Param("maxRiskScore") Integer maxRiskScore,
            Pageable pageable);

    /**
     * 실행자와 무관하게 시뮬레이션 이력 전체를 돌려준다. 관리자 전용 목록에서 쓴다.
     *
     * marketId가 null이면 전체 시장을 대상으로 한다. 네이티브 쿼리라 파라미터 타입을
     * 추론할 수 없어 CAST를 명시한다(캐스트 없이 null을 넘기면 Postgres가
     * "could not determine data type of parameter"로 거절한다).
     *
     * 실행자 조인이 LEFT JOIN인 이유: simscnr01m.user_id를 채우기 전에 만들어진 행이
     * 있어서, INNER JOIN이면 그 이력이 관리자 목록에서도 통째로 사라진다.
     */
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
                   r.generated_report_path AS storageKey,
                   u.name                 AS ownerName,
                   r.predicted_risk_score AS predictedRiskScore
              FROM simrslt01d r
              JOIN simscnr01m s ON s.scenario_id = r.scenario_id
              LEFT JOIN comcode01m c
                     ON c.code_cob = 'POL' AND c.code = s.policy_type_code
              LEFT JOIN mrkaddr01m m ON m.market_id = s.market_id
             LEFT JOIN usrusrs01m u ON u.user_id = s.user_id
             WHERE (CAST(:marketId AS bigint) IS NULL OR s.market_id = CAST(:marketId AS bigint))
               AND (CAST(:withReportOnly AS boolean) = FALSE
                    OR NULLIF(BTRIM(r.generated_report_path), '') IS NOT NULL)
               AND (CAST(:minRiskScore AS int) IS NULL
                    OR r.predicted_risk_score >= CAST(:minRiskScore AS int))
               AND (CAST(:maxRiskScore AS int) IS NULL
                    OR r.predicted_risk_score <= CAST(:maxRiskScore AS int))
               AND (CAST(:keyword AS text) IS NULL
                    OR (CAST(:searchField AS text) IN ('all', 'market')
                        AND m.market_name ILIKE '%' || CAST(:keyword AS text) || '%')
                    OR (CAST(:searchField AS text) IN ('all', 'policy')
                        AND c.code_name ILIKE '%' || CAST(:keyword AS text) || '%')
                    OR (CAST(:searchField AS text) IN ('all', 'reportTitle')
                        AND r.report_title ILIKE '%' || CAST(:keyword AS text) || '%')
                    OR (CAST(:searchField AS text) IN ('all', 'owner')
                        AND u.name ILIKE '%' || CAST(:keyword AS text) || '%'))
             ORDER BY r.executed_at DESC, r.result_id DESC
            """, countQuery = """
            SELECT COUNT(*)
              FROM simrslt01d r
              JOIN simscnr01m s ON s.scenario_id = r.scenario_id
              LEFT JOIN comcode01m c
                     ON c.code_cob = 'POL' AND c.code = s.policy_type_code
              LEFT JOIN mrkaddr01m m ON m.market_id = s.market_id
              LEFT JOIN usrusrs01m u ON u.user_id = s.user_id
             WHERE (CAST(:marketId AS bigint) IS NULL OR s.market_id = CAST(:marketId AS bigint))
               AND (CAST(:withReportOnly AS boolean) = FALSE
                    OR NULLIF(BTRIM(r.generated_report_path), '') IS NOT NULL)
               AND (CAST(:minRiskScore AS int) IS NULL
                    OR r.predicted_risk_score >= CAST(:minRiskScore AS int))
               AND (CAST(:maxRiskScore AS int) IS NULL
                    OR r.predicted_risk_score <= CAST(:maxRiskScore AS int))
               AND (CAST(:keyword AS text) IS NULL
                    OR (CAST(:searchField AS text) IN ('all', 'market')
                        AND m.market_name ILIKE '%' || CAST(:keyword AS text) || '%')
                    OR (CAST(:searchField AS text) IN ('all', 'policy')
                        AND c.code_name ILIKE '%' || CAST(:keyword AS text) || '%')
                    OR (CAST(:searchField AS text) IN ('all', 'reportTitle')
                        AND r.report_title ILIKE '%' || CAST(:keyword AS text) || '%')
                    OR (CAST(:searchField AS text) IN ('all', 'owner')
                        AND u.name ILIKE '%' || CAST(:keyword AS text) || '%'))
            """, nativeQuery = true)
    Page<ReportRow> findScenarioHistory(
            @Param("marketId") Long marketId,
            @Param("keyword") String keyword,
            @Param("searchField") String searchField,
            @Param("withReportOnly") boolean withReportOnly,
            @Param("minRiskScore") Integer minRiskScore,
            @Param("maxRiskScore") Integer maxRiskScore,
            Pageable pageable);

    /**
     * 보고서 저장 결과(S3 키, 문서 제목, 비교에 쓴 현행안 결과)를 결과 행에 기록한다.
     *
     * ScenarioResult 엔티티에는 report_title 매핑이 없어 네이티브 UPDATE로 처리한다.
     * 이 컬럼들은 보고서 기능이 소유하므로, 시뮬레이션 저장을 담당하는 엔티티를
     * 건드리지 않고 여기서만 다루는 편이 경계가 분명하다.
     *
     * baseline_result_id를 함께 남기는 이유: 보고서는 "그 시장의 가장 최근 현행안 결과"와
     * 비교하는데, predict 호출마다 simbsln01d에 결과가 쌓이므로 "가장 최근"이 시점에 따라
     * 달라진다. 같은 시나리오로 다시 만든 보고서의 수치가 바뀌었을 때 어느 현행안과
     * 비교했는지 남아 있지 않으면 원인을 추적할 수 없다. 선택 규칙은 그대로 두고
     * 선택 결과만 기록한다.
     */
    /**
     * previousKey는 이 요청이 보고서를 만들기 시작할 때 읽은 값이다. 그 사이 다른 요청이
     * 먼저 기록했으면 값이 달라져 0행이 갱신되고, 호출자는 그것으로 충돌을 알아챈다.
     *
     * 보고서 생성은 SIM 호출까지 수 분이 걸려 트랜잭션으로 감싸지 않으므로, 두 요청이
     * 겹치면 각자 파일을 올린 뒤 마지막 갱신만 남고 나머지는 참조 없는 객체가 된다.
     * 이 조건이 그 경우를 잡아낸다.
     *
     * IS NOT DISTINCT FROM을 쓰는 이유: 최초 생성이면 양쪽이 NULL인데, '=' 로는
     * NULL = NULL이 참이 되지 않아 항상 0행이 된다.
     */
    @Modifying
    @Transactional
    @Query(value = """
            UPDATE simrslt01d
               SET generated_report_path = :storageKey,
                   report_title = :reportTitle,
                   baseline_result_id = :baselineResultId
             WHERE result_id = :resultId
               AND generated_report_path IS NOT DISTINCT FROM CAST(:previousKey AS varchar)
            """, nativeQuery = true)
    int updateReportInfo(@Param("resultId") Long resultId,
                         @Param("storageKey") String storageKey,
                         @Param("reportTitle") String reportTitle,
                         @Param("baselineResultId") Long baselineResultId,
                         @Param("previousKey") String previousKey);
}
