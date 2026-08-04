package com.markettwin.backend.controller;

import com.markettwin.backend.dto.request.LoginRequestDto;
import com.markettwin.backend.dto.request.ResetPasswordRequestDto;
import com.markettwin.backend.dto.request.SignupRequestDto;
import com.markettwin.backend.dto.request.VerifyIdentityRequestDto;
import com.markettwin.backend.dto.response.LoginResponseDto;
import com.markettwin.backend.dto.response.SignupResponseDto;
import com.markettwin.backend.dto.response.UserSummaryDto;
import com.markettwin.backend.dto.response.VerifyIdentityResponseDto;
import com.markettwin.backend.exception.InvalidCredentialsException;
import com.markettwin.backend.repository.UserRepository;
import com.markettwin.backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 2026-07-24 추가
 * FE authStore.ts의 mock 로그인을 대체할 실제 로그인/회원가입 API.
 * FE 연동 시 authStore.login()의 mock 처리 부분을 아래 두 엔드포인트 호출로 교체하면 됨.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/signup")
    public ResponseEntity<SignupResponseDto> signup(
            @Valid @RequestBody SignupRequestDto request,
            HttpServletRequest httpRequest
    ) {
        SignupResponseDto response = authService.signup(request, resolveClientIp(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public LoginResponseDto login(@Valid @RequestBody LoginRequestDto request) {
        return authService.login(request);
    }

    // 2026-08-04 추가 (비밀번호 찾기 1/2 - 본인확인)
    // FE ForgotPasswordPage와 대응. /api/auth/**가 이미 SecurityConfig에서
    // permitAll이라 별도 설정 변경 없이 로그인 전 상태에서 호출 가능.
    @PostMapping("/verify-identity")
    public VerifyIdentityResponseDto verifyIdentity(@Valid @RequestBody VerifyIdentityRequestDto request) {
        return authService.verifyIdentity(request);
    }

    // 2026-08-04 추가 (비밀번호 찾기 2/2 - 재설정)
    // FE ResetPasswordPage와 대응. 본인확인 4개 필드를 다시 받아 서버가 재검증함.
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDto request,
            HttpServletRequest httpRequest
    ) {
        authService.resetPassword(request, resolveClientIp(httpRequest));
        return ResponseEntity.noContent().build();
    }

    // 2026-07-24 추가: 지금은 SecurityConfig 전체가 permitAll이라 토큰 없이도 호출은
    // 되지만, 인증 정보가 없으면 401을 직접 반환하도록 처리함. FE에서 로그인 유지
    // 여부를 서버에 확인하는 용도로 나중에 붙일 수 있음.
    @GetMapping("/me")
    public ResponseEntity<UserSummaryDto> me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAnonymous = authentication == null
                || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated();
        if (isAnonymous) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String loginId = authentication.getName();
        var user = userRepository.findByLoginId(loginId)
                .orElseThrow(InvalidCredentialsException::new);

        UserSummaryDto summary = UserSummaryDto.builder()
                .userId(user.getUserId())
                .loginId(user.getLoginId())
                .name(user.getName())
                .rulesCode(user.getRulesCode())
                .orgCode(user.getOrgCode())
                .marketCode(user.getMarketCode())
                .build();

        return ResponseEntity.ok(summary);
    }

    // 프록시(로드밸런서 등) 뒤에 있는 경우 X-Forwarded-For를 우선 사용
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
