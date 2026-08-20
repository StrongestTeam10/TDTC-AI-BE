package com.markettwin.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 회원관리 - 관리자가 기존 회원의 rules_code를 변경
//
// 회원관리 화면이 "권한 + 소속 시장"을 한 행에서 같이 고쳐 일괄
// 저장하는 방식으로 바뀌면서 marketCode도 같이 받는다. marketCode는 선택 항목이라
// null이면 "이번 요청에서는 소속 시장을 건드리지 않는다"는 뜻이다(관리자처럼 시장이
// NULL인 회원을 그대로 두는 경우와 구분이 필요해서, 비우려면 빈 문자열을 보낸다).
@Getter
@NoArgsConstructor
public class UpdateUserRoleRequestDto {

    @NotBlank
    private String rulesCode;

    private String marketCode;
}
