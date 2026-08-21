package com.markettwin.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

/**
 * 로그인은 했지만 권한이 모자라 거부된 요청에 403 + JSON으로 응답한다.
 *
 * 이게 없으면 권한 거부가 RestAuthenticationEntryPoint(401)로 흘러간다. FE는 401을
 * "로그인이 풀렸다"로 보고 로그인 상태를 정리하므로(api/client.ts 인터셉터), 조회자
 * 계정이 시뮬레이션 메뉴를 누르면 "권한 없음"이 아니라 로그아웃되는 문제가 생긴다.
 *
 * 401과 403을 구분해야 FE가 "다시 로그인"과 "접근 권한 없음"을 다르게 안내할 수 있다.
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = Map.of(
                "timestamp", Instant.now().toString(),
                "message", "이 기능에 접근할 권한이 없습니다."
        );
        objectMapper.writeValue(response.getWriter(), body);
    }
}
