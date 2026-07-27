package com.markettwin.backend.security;

import com.markettwin.backend.domain.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * 2026-07-24 추가
 * 로그인 성공 시 발급하는 JWT 액세스 토큰의 생성/검증을 담당.
 *
 * ⚠️ jwt.secret은 반드시 32바이트(256비트) 이상이어야 HS256 서명에 사용할 수 있음.
 * application-local.yml에는 로컬 개발용 예시 값이 들어있고, application-prod.yml은
 * 환경변수(JWT_SECRET)로 반드시 별도 설정해야 함 (README/설정 파일 주석 참고).
 */
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long validityMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms:3600000}") long validityMs // 기본 1시간
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.validityMs = validityMs;
    }

    public long getValiditySeconds() {
        return validityMs / 1000;
    }

    public String generateToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validityMs);

        return Jwts.builder()
                .subject(user.getLoginId())
                .claim("userId", user.getUserId())
                .claim("name", user.getName())
                .claim("rulesCode", user.getRulesCode())
                .claim("orgCode", user.getOrgCode())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 토큰을 검증하고 Claims를 반환. 검증 실패(만료/서명불일치/형식오류) 시
     * JwtException을 던짐 - 호출부(JwtAuthenticationFilter)에서 잡아서 인증 없이 통과시킴.
     */
    public Claims parseClaims(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
