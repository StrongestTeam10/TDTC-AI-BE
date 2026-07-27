package com.markettwin.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * USRUSRS01M - 사용자
 */
@Entity
@Table(name = "usrusrs01m")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "login_id", nullable = false, unique = true, length = 30)
    private String loginId;

    @Column(name = "password", nullable = false, length = 64)
    private String password;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "rules_code", nullable = false, length = 5)
    private String rulesCode;

    @Column(name = "org_code", nullable = false, length = 5)
    private String orgCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_ip", nullable = false, length = 16)
    private String createdIp;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "updated_ip", length = 16)
    private String updatedIp;

    // 2026-07-24 추가: FE 회원가입 화면의 동의 항목(서비스 이용약관/개인정보 수집·이용은
    // 필수, 안내사항 수신은 선택)과 대응하는 동의 이력. 필수 두 개는 회원가입 시점에
    // 항상 값이 채워지지만, 기존 데이터와의 호환을 위해 컬럼 자체는 NOT NULL로 걸지
    // 않음(다른 컬럼 추가 때와 동일한 방식 - ALTER TABLE ADD COLUMN IF NOT EXISTS 참고).
    @Column(name = "agree_terms_at")
    private Instant agreeTermsAt;

    @Column(name = "agree_privacy_at")
    private Instant agreePrivacyAt;

    // 선택 동의라 미동의 시 NULL
    @Column(name = "agree_marketing_at")
    private Instant agreeMarketingAt;

    // 2026-07-24 추가(게시판): 담당 시장 코드(comcode01m MKT 도메인). 게시판 목록에서
    // "본인 담당 시장 글만 노출" 판정 기준. 관리자(ROL01)는 시장 제한이 없어 NULL이어도
    // 무방. 회원가입 화면에서 org_code와 동일하게 select로 입력받고, AuthService가
    // MKT 도메인 코드로 존재하는지 검증함.
    @Column(name = "market_code", length = 5)
    private String marketCode;
}
