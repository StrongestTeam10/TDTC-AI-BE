package com.markettwin.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

/**
 * 2026-07-24 추가
 * 인증되지 않은 요청이 authenticated()로 보호된 API에 접근했을 때 Spring Security
 * 기본 동작(403 Forbidden, 빈 본문) 대신 FE가 처리하기 좋은 401 + JSON 본문으로
 * 응답하기 위한 진입점. FE api/client.ts는 401을 보고 로그인 상태를 정리함.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> body = Map.of(
                "timestamp", Instant.now().toString(),
                "message", "로그인이 필요합니다."
        );
        objectMapper.writeValue(response.getWriter(), body);
    }
}
