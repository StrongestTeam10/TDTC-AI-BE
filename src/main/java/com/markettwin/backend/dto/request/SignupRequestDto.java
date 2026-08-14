package com.markettwin.backend.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 2026-07-24 추가
 * FE SignupPage(회원가입 화면) 입력값과 대응. 비밀번호 조합 규칙(8자 이상 + 영문
 * 대/소문자·숫자·특수문자 모두 포함)은 FE utils/password.ts와 동일한 정책을 서버에서도
 * 한 번 더 검증함(클라이언트 검증만 믿지 않음).
 */
@Getter
@NoArgsConstructor
public class SignupRequestDto {

    @NotBlank(message = "아이디는 필수입니다.")
    @Size(max = 30, message = "아이디는 30자 이하여야 합니다.")
    private String loginId;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
            message = "비밀번호는 8자 이상이며 영문 대문자·소문자·숫자·특수문자를 모두 포함해야 합니다."
    )
    private String password;

    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
    private String name;

    // 2026-08-13 추가: 전화번호 (선택 항목)
    @Size(max = 20, message = "전화번호는 20자 이하여야 합니다.")
    private String phoneNumber;

    // comcode01m의 ORG 도메인 코드(예: ORGKT, ORGGV, ORGMA) 중 하나여야 함.
    // 실제 존재 여부는 AuthService에서 CommonCodeRepository로 한 번 더 확인함.
    @NotBlank(message = "소속기관은 필수입니다.")
    @Size(max = 5)
    private String orgCode;

    // 2026-07-24 추가(게시판): comcode01m의 MKT 도메인 코드(예: MKTMW) 중 하나여야 함.
    // 게시판 목록 노출 범위("본인 담당 시장 글만 조회") 기준이 되는 필드라 필수로 받음.
    // 실제 존재 여부는 orgCode와 동일하게 AuthService에서 한 번 더 확인함.
    @NotBlank(message = "담당 시장은 필수입니다.")
    @Size(max = 5)
    private String marketCode;

    // 필수 동의 2개는 반드시 true여야 함(체크 안 하면 검증 실패)
    @AssertTrue(message = "서비스 이용약관에 동의해야 합니다.")
    private boolean agreeTerms;

    @AssertTrue(message = "개인정보 수집 및 이용에 동의해야 합니다.")
    private boolean agreePrivacy;

    // 선택 동의라 true/false 둘 다 허용
    private boolean agreeMarketing;
}
