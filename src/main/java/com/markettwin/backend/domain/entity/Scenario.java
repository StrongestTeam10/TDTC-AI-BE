package com.markettwin.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * SIMSCNR01M - 시나리오 (파이프라인 B: 사용자 지정 시뮬레이션 요청)
 *
 * 2026-07-27 수정: spaceModData에 @JdbcTypeCode(SqlTypes.JSON) 추가.
 * columnDefinition="jsonb"만으로는 Hibernate가 실제 저장 시 문자열(character
 * varying)로 취급해서 "column is of type jsonb but expression is of type
 * character varying" 에러가 났음 - 이 어노테이션이 있어야 Hibernate가 값을
 * 실제로 jsonb로 변환해서 보낸다.
 */
@Entity
@Table(name = "simscnr01m")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Scenario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scenario_id")
    private Long scenarioId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "scenario_name", nullable = false, length = 100)
    private String scenarioName;

    @Column(name = "market_id")
    private Long marketId;

    @Column(name = "virtual_config", nullable = false, columnDefinition = "TEXT")
    private String virtualConfig;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "space_mod_data", columnDefinition = "jsonb")
    private String spaceModData;

    @Column(name = "reg_datetime", nullable = false)
    private Instant regDatetime;

    @Column(name = "agent_count", nullable = false)
    private Integer agentCount;

    @Column(name = "policy_type_code", nullable = false, length = 5)
    private String policyTypeCode;

    @Column(name = "created_at")
    private Instant createdAt;
}