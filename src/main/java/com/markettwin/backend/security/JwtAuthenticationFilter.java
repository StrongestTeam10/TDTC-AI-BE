package com.markettwin.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authorization: Bearer {token} 헤더를 읽어 유효하면 SecurityContext에 인증 정보를
 * 세팅함. 지금은 SecurityConfig에서 대부분의 API를 permitAll로 열어뒀기 때문에
 * "필수"는 아니지만, /api/auth/me 같은 보호된 엔드포인트와 향후 기존 API를 잠글 때를
 * 대비해 미리 붙여둠. 토큰이 없거나 유효하지 않아도 요청 자체는 그대로 통과시키고
 * (permitAll 엔드포인트는 인증 없이도 접근 가능해야 하므로), 인증이 실제로 필요한
 * 엔드포인트는 SecurityConfig의 authorizeHttpRequests에서 authenticated()로 지정하면 됨.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring("Bearer ".length());
            try {
                Claims claims = jwtTokenProvider.parseClaims(token);
                String loginId = claims.getSubject();
                String rulesCode = claims.get("rulesCode", String.class);

                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + rulesCode));
                var authentication =
                        new UsernamePasswordAuthenticationToken(loginId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException e) {
                // 토큰이 만료/위조된 경우: 로그만 남기고 인증 없이 진행(permitAll 경로는
                // 그대로 접근 가능해야 하고, authenticated() 경로는 이후 단계에서 401 처리됨)
                log.debug("JWT 검증 실패: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
