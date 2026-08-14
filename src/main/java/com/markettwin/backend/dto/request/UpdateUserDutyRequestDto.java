package com.markettwin.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 2026-08-13 추가: 관리자가 당직 상태를 변경할 때 사용하는 DTO
@Getter
@NoArgsConstructor
public class UpdateUserDutyRequestDto {

    @NotNull(message = "당직 여부 값은 필수입니다.")
    private Boolean isDuty;
}
