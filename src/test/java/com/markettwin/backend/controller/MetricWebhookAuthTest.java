package com.markettwin.backend.controller;

import com.markettwin.backend.config.SecurityConfig;
import com.markettwin.backend.dto.request.MetricBulkRequest;
import com.markettwin.backend.security.JwtTokenProvider;
import com.markettwin.backend.security.RestAccessDeniedHandler;
import com.markettwin.backend.security.RestAuthenticationEntryPoint;
import com.markettwin.backend.service.MetricBatchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 메트릭 벌크 웹훅(POST /api/v1/metrics/bulk)의 X-API-KEY 인증을 확인한다.
 *
 * 거부만이 아니라 통과도 함께 고정한다. 잘못 막으면 AI 파이프라인의 지표 적재가
 * 통째로 멈추기 때문이다. DB는 띄우지 않고 서비스는 목으로 둔다.
 */
@WebMvcTest(MetricWebhookController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "ai.secret-key=test-secret-key-1234567890",
        "jwt.secret=test-jwt-secret-key-for-webmvc-slice-0000",
        "cors.allowed-origins=http://localhost:5173"
})
class MetricWebhookAuthTest {

    private static final String VALID_KEY = "test-secret-key-1234567890";

    private static final String PAYLOAD = """
            {
              "zoneId": 3,
              "clipId": 12,
              "frames": [
                { "frameId": 1, "videoId": 7, "totalCount": 5,
                  "riskScore": 42.5, "riskLevel": "LVL02",
                  "capturedAt": "2026-01-01T00:00:00Z" }
              ]
            }
            """;

    @Autowired
    private MockMvc mvc;

    @MockBean
    private MetricBatchService metricBatchService;

    // SecurityConfig 가 요구하는 협력자들
    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    @MockBean
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    @MockBean
    private RestAccessDeniedHandler restAccessDeniedHandler;

    @Test
    @DisplayName("정상 키면 200이고 적재가 호출된다")
    void validKeyIsAccepted() throws Exception {
        given(metricBatchService.bulkInsertMetrics(any(MetricBulkRequest.class)))
                .willReturn(Map.of("inserted", 1));

        mvc.perform(post("/api/v1/metrics/bulk")
                        .header("X-API-KEY", VALID_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD))
                .andExpect(status().isOk());

        verify(metricBatchService).bulkInsertMetrics(any(MetricBulkRequest.class));
    }

    @Test
    @DisplayName("틀린 키는 401이고 적재하지 않는다")
    void wrongKeyIsRejected() throws Exception {
        mvc.perform(post("/api/v1/metrics/bulk")
                        .header("X-API-KEY", "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD))
                .andExpect(status().isUnauthorized());

        verify(metricBatchService, never()).bulkInsertMetrics(any());
    }

    @Test
    @DisplayName("헤더가 없으면 401이다")
    void missingHeaderIsRejected() throws Exception {
        mvc.perform(post("/api/v1/metrics/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD))
                .andExpect(status().isUnauthorized());

        verify(metricBatchService, never()).bulkInsertMetrics(any());
    }

    @Test
    @DisplayName("빈 헤더도 401이다")
    void emptyHeaderIsRejected() throws Exception {
        mvc.perform(post("/api/v1/metrics/bulk")
                        .header("X-API-KEY", "")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD))
                .andExpect(status().isUnauthorized());

        verify(metricBatchService, never()).bulkInsertMetrics(any());
    }

    @Test
    @DisplayName("설정 키의 앞부분만 맞춰도 401이다")
    void partialKeyIsRejected() throws Exception {
        mvc.perform(post("/api/v1/metrics/bulk")
                        .header("X-API-KEY", VALID_KEY.substring(0, 10))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD))
                .andExpect(status().isUnauthorized());

        verify(metricBatchService, never()).bulkInsertMetrics(any());
    }
}
