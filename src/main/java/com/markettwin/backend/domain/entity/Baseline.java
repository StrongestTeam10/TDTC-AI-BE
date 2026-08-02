package com.markettwin.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * SIMBSLN01M - 현행안
 *
 * 2026-07-27 신규(ERD 반영). 시나리오(대안, Scenario)와 달리, 현행안은 "지금 실제로
 * 배치돼 있는 시설(Facility)/외부요인(ExternalFactor)을 그대로 반영해서" 돌리는
 * 기준선(baseline) 시뮬레이션. 실행 시점에 그 시장의 활성화된 시설/외부요인을 조회해서
 * 반영하는 방식이라 이 엔티티 자체가 Facility/ExternalFactor를 직접 FK로 참조하진
 * 않음(런타임 조인).
 */
@Entity
@Table(name = "simbsln01m")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Baseline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "baseline_id")
    private Long baselineId;

    @Column(name = "market_id", nullable = false)
    private Long marketId;

    @Column(name = "baseline_name", length = 100)
    private String baselineName;

    @Column(name = "virtual_config", nullable = false, columnDefinition = "TEXT")
    private String virtualConfig;

    @Column(name = "policy_type_code", nullable = false, length = 5)
    @Builder.Default
    private String policyTypeCode = "POLNO";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "reg_datetime", nullable = false)
    private Instant regDatetime;

    @Column(name = "created_at")
    private Instant createdAt;
}
