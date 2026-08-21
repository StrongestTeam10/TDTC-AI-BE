package com.markettwin.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * MRKOBJT01M - 시장 오브젝트/구조 설정 (신규)
 *
 * 시뮬레이션 비교의 초기 배치로 쓸 "오브젝트 배치 + 통로 제어 정책"을 시장 구조 등록
 * 화면에서 미리 등록해두는 마스터. 시장당 1행이다.
 *
 * 두 JSON은 각각 PlacedObjectDto[] / CorridorPolicyDto[]를 문자열로 담는다. 정규화하지
 * 않고 통째로 저장/조회만 하며, 형식이 시뮬레이션 요청과 1:1이라 변환 없이 오간다.
 */
@Entity
@Table(name = "mrkobjt01m")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketObjectConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "config_id")
    private Long configId;

    @Column(name = "market_id", nullable = false)
    private Long marketId;

    /** PlacedObjectDto[] JSON 문자열. 빈 배열이면 "[]". */
    @Column(name = "objects_json", columnDefinition = "TEXT", nullable = false)
    private String objectsJson;

    /** CorridorPolicyDto[] JSON 문자열. 빈 배열이면 "[]". */
    @Column(name = "corridor_policies_json", columnDefinition = "TEXT", nullable = false)
    private String corridorPoliciesJson;

    @Column(name = "updated_at")
    private Instant updatedAt;

    /** 시장당 1행을 통째로 갈아끼운다(화면이 전체 세트를 덮어쓰기 하므로). */
    public void updateConfig(String objectsJson, String corridorPoliciesJson) {
        this.objectsJson = objectsJson;
        this.corridorPoliciesJson = corridorPoliciesJson;
        this.updatedAt = Instant.now();
    }
}
