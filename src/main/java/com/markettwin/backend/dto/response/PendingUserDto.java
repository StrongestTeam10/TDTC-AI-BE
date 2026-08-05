package com.markettwin.backend.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * 2026-08-04 추가 (회원가입 관리자 승인)
 */
@Getter
@Builder
public class PendingUserDto {
    private Long userId;
    private String loginId;
    private String name;
    private String orgCode;
    private String marketCode;
    private String approvalStatus;
    private Instant createdAt;
}
