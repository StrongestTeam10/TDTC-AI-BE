package com.markettwin.backend.controller;

import com.markettwin.backend.dto.request.AlertTriggerRequest;
import com.markettwin.backend.domain.entity.EmergencyAlert;
import com.markettwin.backend.domain.entity.PedestrianCoordinateJson;
import com.markettwin.backend.service.ControlSystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiIntegrationController {

    private final ControlSystemService controlSystemService;

    // application.yml에 적어둔 비밀번호를 몰래 가져옵니다.
    @Value("${ai.secret-key}")
    private String aiSecretKey;

    // 미해결 긴급 신고 목록 조회
    @GetMapping("/alerts/unresolved")
    public List<EmergencyAlert> getUnresolvedAlerts() {
        return controlSystemService.getUnresolvedAlerts();
    }

    // 보행자 좌표 조회
    @GetMapping("/coordinates-json")
    public List<PedestrianCoordinateJson> getJsonCoordinates(
            @RequestParam Long clipId,
            @RequestParam Integer frameId,
            @RequestParam(required = false) Integer videoId) {
        return controlSystemService.getJsonCoordinates(clipId, frameId, videoId);
    }

    // 🚨 파이썬(AI)에서 10초 이상 위험 감지 시 호출하는 전용 트리거 API (문지기 락 추가)
    @PostMapping("/alerts/trigger")
    public ResponseEntity<String> triggerEmergencyAlert(
            @RequestHeader(value = "X-API-KEY", required = false) String apiKey, // 👈 헤더에서 키 검사
            @RequestBody AlertTriggerRequest request) {

        // 1. 열쇠 검사 (비밀번호가 안 들어왔거나 틀리면 401 에러와 함께 문전박대)
        if (apiKey == null || !apiKey.equals(aiSecretKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("❌ 경고: API 키가 일치하지 않거나 누락되었습니다. (해킹 시도 차단)");
        }

        // 2. 열쇠가 맞으면 정상 처리 통과
        controlSystemService.triggerAlertFromAi(request.zoneId(), request.alertType());
        return ResponseEntity.ok("✅ 긴급 알람 처리 프로세스가 성공적으로 시작되었습니다.");
    }
}
