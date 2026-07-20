package com.markettwin.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * MRKFCTS01M - 시설
 */
@Entity
@Table(name = "MRKFCTS01M")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Facility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "facility_id")
    private Long facilityId;

    @Column(name = "market_id", nullable = false)
    private Long marketId;

    @Column(name = "facility_type", nullable = false, length = 30)
    private String facilityType;   // 위험/화재변 등

    @Column(name = "name", nullable = false, length = 30)
    private String name;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
