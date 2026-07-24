package com.markettwin.backend.dto.response;

import lombok.Builder;
import lombok.Getter;

// 2026-07-24 추가
// FE authStore.ts의 AuthUser 타입과 필드명을 맞춰둠 (loginId/name/rulesCode/orgCode)
@Getter
@Builder
public class UserSummaryDto {
    private Long userId;
    private String loginId;
    private String name;
    private String rulesCode;
    private String orgCode;
}
