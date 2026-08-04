package com.markettwin.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 2026-08-04 추가 (비밀번호 찾기)
 *
 * usrusrs01m에 이메일/휴대폰 컬럼이 없어 이메일 인증코드 방식을 쓸 수 없음. 대신
 * 회원가입 때 입력한 필드(아이디+이름+소속기관+담당시장) 4개가 모두 일치하는지만
 * 확인한다 - 지금 있는 필드만 사용, 스키마 변경 없이 구현하기로 한 결정.
 */
@Getter
@NoArgsConstructor
public class VerifyIdentityRequestDto {

    @NotBlank(message = "아이디는 필수입니다.")
    private String loginId;

    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    @NotBlank(message = "소속기관은 필수입니다.")
    private String orgCode;

    @NotBlank(message = "담당 시장은 필수입니다.")
    private String marketCode;
}
