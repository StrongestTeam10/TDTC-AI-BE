package com.markettwin.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * MRKRISK01M - 위험 점수
 *
 * 2026-07-27 변경(ERD 반영): 그레인이 "구역 단위(zone_id)"에서 "CCTV 프레임의
 * 좌표 1건 단위(coord_id, PedestrianCoordinate 참조)"로 바뀜. 기존 zone_id 기반
 * 데이터는 DB에서 mrkrisk01m_zone_legacy로 보존해뒀고(schema-init.sql 참고),
 * 이 엔티티는 새 mrkrisk01m 테이블에 매핑됨.
 *
 * 참고: 이 엔티티는 원래(수정 전) zoneId + frameId(NOT NULL) 조합으로 되어 있었는데,
 * 그건 현재 DB 어느 스키마와도 일치하지 않는 중간 상태였음(zone_id 기반 옛 스키마엔
 * frame_id가 없었고, 이번 ERD엔 zone_id/frame_id 둘 다 없음) - ddl-auto: validate라
 * 그 상태로는 서버 기동이 실패했을 것. 이번에 coord_id 기반으로 정리함.
 */
@Entity
@Table(name = "mrkrisk01m")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Risk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "risk_id")
    private Long riskId;

    @Column(name = "coord_id", nullable = false)
    private Long coordId;

    @Column(name = "risk_score", nullable = false)
    private Float riskScore;

    @Column(name = "risk_level", nullable = false, length = 10)
    private String riskLevel;

    @Column(name = "reason_code", nullable = false, length = 200)
    private String reasonCode;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(name = "total_count")
    private Integer totalCount;

    @Column(name = "video_url", length = 1000)
    private String videoUrl;
}
