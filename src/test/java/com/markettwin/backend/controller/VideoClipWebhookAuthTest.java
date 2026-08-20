package com.markettwin.backend.controller;

import com.markettwin.backend.config.SecurityConfig;
import com.markettwin.backend.domain.entity.VideoClip;
import com.markettwin.backend.repository.VideoClipRepository;
import com.markettwin.backend.security.JwtTokenProvider;
import com.markettwin.backend.security.RestAccessDeniedHandler;
import com.markettwin.backend.security.RestAuthenticationEntryPoint;
import com.markettwin.backend.service.VideoS3Service;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AI 파이프라인 웹훅이 HTTP 단에서 어떻게 응답하는지 확인한다(보안 감사 BE-05).
 *
 * ApiKeysTest 는 키 비교 로직만 본다. 여기서는 컨트롤러·시큐리티 필터를 실제로 태워
 * "정상 키면 200이고 저장까지 되는가", "틀린 키면 401이고 저장이 없는가"를 확인한다.
 * 잘못 막으면 CCTV 파이프라인이 통째로 멈추는 자리라, 거부만이 아니라 통과도 함께
 * 고정해 둔다.
 *
 * DB 는 띄우지 않는다(리포지토리는 목). 개발 DB 에 테스트 클립이 쌓이지 않게 하려는
 * 목적도 있다.
 */
@WebMvcTest(VideoClipController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "ai.secret-key=test-secret-key-1234567890",
        "jwt.secret=test-jwt-secret-key-for-webmvc-slice-0000",
        "cors.allowed-origins=http://localhost:5173"
})
class VideoClipWebhookAuthTest {

    private static final String VALID_KEY = "test-secret-key-1234567890";

    private static final String PAYLOAD = """
            {
              "zoneId": 3,
              "clipType": "RISK",
              "s3ClipUrl": "s3://tdtc-cctv-upload/danger-clips/x.mp4",
              "startTime": "2026-08-20T03:00:00Z",
              "endTime": "2026-08-20T03:00:35Z"
            }
            """;

    @Autowired
    private MockMvc mvc;

    @MockBean
    private VideoClipRepository repository;

    @MockBean
    private VideoS3Service videoS3Service;

    // SecurityConfig 가 생성자 인자로 요구하는 협력자들.
    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    @MockBean
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    @MockBean
    private RestAccessDeniedHandler restAccessDeniedHandler;

    private void givenSaveReturnsId(long clipId) {
        given(repository.save(any(VideoClip.class)))
                .willAnswer(inv -> {
                    VideoClip c = inv.getArgument(0);
                    return VideoClip.builder()
                            .clipId(clipId)
                            .zoneId(c.getZoneId())
                            .clipType(c.getClipType())
                            .s3ClipUrl(c.getS3ClipUrl())
                            .startTime(c.getStartTime())
                            .endTime(c.getEndTime())
                            .build();
                });
    }

    @Test
    @DisplayName("정상 키면 200이고 클립이 저장된다 - 파이프라인이 계속 도는지")
    void validKeyIsAccepted() throws Exception {
        givenSaveReturnsId(12L);

        mvc.perform(post("/api/v1/video-clips")
                        .header("X-API-KEY", VALID_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clipId").value(12));

        verify(repository).save(any(VideoClip.class));
    }

    @Test
    @DisplayName("틀린 키는 401이고 아무것도 저장하지 않는다")
    void wrongKeyIsRejected() throws Exception {
        mvc.perform(post("/api/v1/video-clips")
                        .header("X-API-KEY", "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD))
                .andExpect(status().isUnauthorized());

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("헤더가 없으면 401이다")
    void missingHeaderIsRejected() throws Exception {
        mvc.perform(post("/api/v1/video-clips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD))
                .andExpect(status().isUnauthorized());

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("빈 헤더도 401이다 - 예전에 뚫렸던 경로")
    void emptyHeaderIsRejected() throws Exception {
        mvc.perform(post("/api/v1/video-clips")
                        .header("X-API-KEY", "")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD))
                .andExpect(status().isUnauthorized());

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("설정 키의 앞부분만 맞춰도 401이다")
    void partialKeyIsRejected() throws Exception {
        mvc.perform(post("/api/v1/video-clips")
                        .header("X-API-KEY", VALID_KEY.substring(0, 10))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD))
                .andExpect(status().isUnauthorized());

        verify(repository, never()).save(any());
    }
}
