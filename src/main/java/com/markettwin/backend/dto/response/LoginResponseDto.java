package com.markettwin.backend.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponseDto {
    private String accessToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private long expiresInSeconds;
    private UserSummaryDto user;
}
