package com.markettwin.backend.dto.response;

import lombok.Builder;
import lombok.Getter;

// 2026-07-24 추가
@Getter
@Builder
public class LoginResponseDto {
    private String accessToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private long expiresInSeconds;
    private UserSummaryDto user;
}
