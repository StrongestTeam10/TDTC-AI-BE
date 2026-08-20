package com.markettwin.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 비밀번호 찾기
 *
 * 본인확인 4개 필드(아이디+이름+소속기관+담당시장)를 재설정 요청에도 함께 받아
 * 서버가 다시 한 번 검증한다(FE가 verify-identity 단계를 건너뛰고 이 API를 바로
 * 호출하더라도, 4개 필드가 실제로 일치하지 않으면 거부됨 - FE의 라우터 state만
 * 믿지 않는 방어적 설계). 비밀번호 조합 규칙은 SignupRequestDto와 동일한 정책.
 */
@Getter
@NoArgsConstructor
public class ResetPasswordRequestDto {

    @NotBlank(message = "아이디는 필수입니다.")
    private String loginId;

    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    @NotBlank(message = "소속기관은 필수입니다.")
    private String orgCode;

    @NotBlank(message = "담당 시장은 필수입니다.")
    private String marketCode;

    @NotBlank(message = "새 비밀번호는 필수입니다.")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
            message = "비밀번호는 8자 이상이며 영문 대문자·소문자·숫자·특수문자를 모두 포함해야 합니다."
    )
    private String newPassword;
}
