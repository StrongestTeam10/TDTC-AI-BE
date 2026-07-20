package com.markettwin.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * SIMSCNR01M - 시나리오 (파이프라인 B: 사용자 지정 시뮬레이션 요청)
 */
@Entity
@Table(name = "SIMSCNR01M")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Scenario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scenario_id")
    private Long scenarioId;

    @Column(name = "change_id")
    private Long changeId;

    @Column(name = "scenario_name", nullable = false, length = 100)
    private String scenarioName;

    @Column(name = "market_id")
    private Long marketId;

    @Lob
    @Column(name = "virtual_config", nullable = false)
    private String virtualConfig;

    @Column(name = "space_mod_data", columnDefinition = "jsonb")
    private String spaceModData;

    @Column(name = "reg_datetime", nullable = false)
    private Instant regDatetime;

    @Column(name = "agent_count")
    private Integer agentCount;

    @Column(name = "policy_type_code", length = 5)
    private String policyTypeCode;

    @Column(name = "created_at")
    private Instant createdAt;
}
