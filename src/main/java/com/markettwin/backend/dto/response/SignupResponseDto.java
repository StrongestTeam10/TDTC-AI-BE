package com.markettwin.backend.dto.response;

import lombok.Builder;
import lombok.Getter;

// 2026-07-24 추가
@Getter
@Builder
public class SignupResponseDto {
    private Long userId;
    private String loginId;
    private String name;
}
