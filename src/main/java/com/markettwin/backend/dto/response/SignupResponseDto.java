package com.markettwin.backend.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SignupResponseDto {
    private Long userId;
    private String loginId;
    private String name;
}
