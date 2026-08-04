package com.markettwin.backend.service;

import com.markettwin.backend.domain.entity.User;
import com.markettwin.backend.dto.request.LoginRequestDto;
import com.markettwin.backend.dto.request.ResetPasswordRequestDto;
import com.markettwin.backend.dto.request.SignupRequestDto;
import com.markettwin.backend.dto.request.VerifyIdentityRequestDto;
import com.markettwin.backend.dto.response.LoginResponseDto;
import com.markettwin.backend.dto.response.SignupResponseDto;
import com.markettwin.backend.dto.response.UserSummaryDto;
import com.markettwin.backend.dto.response.VerifyIdentityResponseDto;
import com.markettwin.backend.exception.DuplicateLoginIdException;
import com.markettwin.backend.exception.IdentityVerificationFailedException;
import com.markettwin.backend.exception.InvalidCredentialsException;
import com.markettwin.backend.exception.InvalidMarketCodeException;
import com.markettwin.backend.exception.InvalidOrgCodeException;
import com.markettwin.backend.repository.CommonCodeRepository;
import com.markettwin.backend.repository.UserRepository;
import com.markettwin.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 2026-07-24 추가
 * 회원가입/로그인 처리. FE authStore.ts의 mock 로그인을 대체하는 실제 BE 구현.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    // 2026-07-24: 자가 가입 시 부여하는 기본 권한. comcode-seed.sql 기준
    // ROL03 = 조회자(최소 권한). 관리자(ROL01)/관제요원(ROL02)으로의 승격은
    // 별도 관리자 승인 절차가 필요하며, 이번 범위에는 포함하지 않음.
    private static final String DEFAULT_ROLE_CODE = "ROL03";

    // 2026-07-24: comcode01m.code_cob(공통코드분류) 값. 처음엔 code.startsWith("ORG")
    // 문자열 매칭으로 임시 검증했는데, code_cob 컬럼이 추가되면서 실제 컬럼 기반
    // 조회(existsByCodeCobAndCode)로 교체함.
    private static final String ORG_CODE_COB = "ORG";

    // 2026-07-24 추가(게시판): comcode01m.code_cob 값(MKT 도메인). ORG_CODE_COB와
    // 동일한 패턴으로 signup 요청의 marketCode가 실제 존재하는 코드인지 검증함.
    private static final String MARKET_CODE_COB = "MKT";

    private final UserRepository userRepository;
    private final CommonCodeRepository commonCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public SignupResponseDto signup(SignupRequestDto request, String clientIp) {
        validateOrgCode(request.getOrgCode());
        validateMarketCode(request.getMarketCode());

        if (userRepository.existsByLoginId(request.getLoginId())) {
            throw new DuplicateLoginIdException(request.getLoginId());
        }

        Instant now = Instant.now();

        User user = User.builder()
                .loginId(request.getLoginId())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .rulesCode(DEFAULT_ROLE_CODE)
                .orgCode(request.getOrgCode())
                .marketCode(request.getMarketCode())
                .createdAt(now)
                .createdIp(clientIp)
                .agreeTermsAt(now)
                .agreePrivacyAt(now)
                .agreeMarketingAt(request.isAgreeMarketing() ? now : null)
                .build();

        User saved = userRepository.save(user);

        return SignupResponseDto.builder()
                .userId(saved.getUserId())
                .loginId(saved.getLoginId())
                .name(saved.getName())
                .build();
    }

    public LoginResponseDto login(LoginRequestDto request) {
        User user = userRepository.findByLoginId(request.getLoginId())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtTokenProvider.generateToken(user);

        return LoginResponseDto.builder()
                .accessToken(token)
                .expiresInSeconds(jwtTokenProvider.getValiditySeconds())
                .user(toSummary(user))
                .build();
    }

    // 2026-08-04 추가 (비밀번호 찾기)
    // 아이디+이름+소속기관+담당시장 4개가 모두 일치하는 계정이 있는지만 확인하고,
    // 존재 여부를 예외가 아니라 응답 필드(verified)로 알림(계정 존재 여부를 HTTP
    // 상태코드로 노출하지 않기 위함).
    public VerifyIdentityResponseDto verifyIdentity(VerifyIdentityRequestDto request) {
        boolean verified = userRepository.findByLoginIdAndNameAndOrgCodeAndMarketCode(
                request.getLoginId(), request.getName(), request.getOrgCode(), request.getMarketCode()
        ).isPresent();

        return VerifyIdentityResponseDto.builder().verified(verified).build();
    }

    // 2026-08-04 추가 (비밀번호 찾기)
    // FE가 verify-identity 단계를 건너뛰고 이 API를 바로 호출했더라도, 여기서 다시
    // 한 번 4개 필드 일치 여부를 검증한다(FE 라우터 state 조작에 대한 방어).
    @Transactional
    public void resetPassword(ResetPasswordRequestDto request, String clientIp) {
        User user = userRepository.findByLoginIdAndNameAndOrgCodeAndMarketCode(
                request.getLoginId(), request.getName(), request.getOrgCode(), request.getMarketCode()
        ).orElseThrow(IdentityVerificationFailedException::new);

        user.updatePassword(passwordEncoder.encode(request.getNewPassword()), clientIp);
    }

    private void validateOrgCode(String orgCode) {
        boolean valid = orgCode != null
                && commonCodeRepository.existsByCodeCobAndCode(ORG_CODE_COB, orgCode);
        if (!valid) {
            throw new InvalidOrgCodeException(orgCode);
        }
    }

    private void validateMarketCode(String marketCode) {
        boolean valid = marketCode != null
                && commonCodeRepository.existsByCodeCobAndCode(MARKET_CODE_COB, marketCode);
        if (!valid) {
            throw new InvalidMarketCodeException(marketCode);
        }
    }

    private UserSummaryDto toSummary(User user) {
        return UserSummaryDto.builder()
                .userId(user.getUserId())
                .loginId(user.getLoginId())
                .name(user.getName())
                .rulesCode(user.getRulesCode())
                .orgCode(user.getOrgCode())
                .marketCode(user.getMarketCode())
                .build();
    }
}
