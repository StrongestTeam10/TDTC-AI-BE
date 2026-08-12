package com.markettwin.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * MRKCCTV01M - CCTV 관제 구역 (2026-08-11 신규, 08-11 2차 재설계)
 *
 * ⚠️ 시뮬레이션 구역({@link Zone}, MRKADDR01D)과 의도적으로 분리된 테이블이다.
 * MRKADDR01D는 SIM이 직접 읽어 위험도를 계산하므로 CCTV 구역을 섞으면 결과가 바뀐다.
 *
 * 2차 재설계: 고정 슬롯(zone_no 1~4)을 버리고, 시뮬레이션 구역(zoneId)에 소속되는
 * 행을 등록마다 추가하는 구조가 됐다. 목록의 구역명은 zoneId로 MRKADDR01D를 조인해
 * 가져오므로 이 엔티티에는 구역명을 두지 않는다.
 */
@Entity
@Table(name = "mrkcctv01m")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CctvZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cctv_zone_id")
    private Long cctvZoneId;

    @Column(name = "market_id", nullable = false)
    private Long marketId;

    /** 소속 시뮬레이션 구역(MRKADDR01D.zone_id). 이 구역 폴리곤 안에서만 그릴 수 있다. */
    @Column(name = "zone_id", nullable = false)
    private Long zoneId;

    /**
     * GeoJSON Polygon 문자열. MRKADDR01D.polygon_coordinates와 같은 형식이라
     * FE의 폴리곤 파싱 코드를 그대로 재사용할 수 있다.
     */
    @Column(name = "polygon_coordinates", columnDefinition = "TEXT", nullable = false)
    private String polygonCoordinates;

    /** CCTV 사용/미사용. 미사용이면 파이프라인이 분석 대상에서 뺀다. */
    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "rmk", length = 500)
    private String rmk;

    @Column(name = "updated_at")
    private Instant updatedAt;

    /** 기존 CCTV 구역을 수정할 때 내용을 갈아끼운다. */
    public void updateDetails(Long zoneId, String polygonCoordinates, Boolean isActive, String rmk) {
        this.zoneId = zoneId;
        this.polygonCoordinates = polygonCoordinates;
        this.isActive = isActive;
        this.rmk = rmk;
        this.updatedAt = Instant.now();
    }
}
