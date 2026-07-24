package com.markettwin.backend.service;

import com.markettwin.backend.domain.entity.User;
import com.markettwin.backend.dto.request.LoginRequestDto;
import com.markettwin.backend.dto.request.SignupRequestDto;
import com.markettwin.backend.dto.response.LoginResponseDto;
import com.markettwin.backend.dto.response.SignupResponseDto;
import com.markettwin.backend.dto.response.UserSummaryDto;
import com.markettwin.backend.exception.DuplicateLoginIdException;
import com.markettwin.backend.exception.InvalidCredentialsException;
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

    private final UserRepository userRepository;
    private final CommonCodeRepository commonCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public SignupResponseDto signup(SignupRequestDto request, String clientIp) {
        validateOrgCode(request.getOrgCode());

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

    private void validateOrgCode(String orgCode) {
        boolean valid = orgCode != null
                && commonCodeRepository.existsByCodeCobAndCode(ORG_CODE_COB, orgCode);
        if (!valid) {
            throw new InvalidOrgCodeException(orgCode);
        }
    }

    private UserSummaryDto toSummary(User user) {
        return UserSummaryDto.builder()
                .userId(user.getUserId())
                .loginId(user.getLoginId())
                .name(user.getName())
                .rulesCode(user.getRulesCode())
                .orgCode(user.getOrgCode())
                .build();
    }
}
