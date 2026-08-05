package com.markettwin.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 2026-08-05 추가 (회원관리 - 관리자가 기존 회원의 rules_code를 변경)
@Getter
@NoArgsConstructor
public class UpdateUserRoleRequestDto {

    @NotBlank
    private String rulesCode;
}
