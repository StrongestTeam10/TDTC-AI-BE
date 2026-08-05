package com.markettwin.backend.controller;

import com.markettwin.backend.dto.request.AlertTriggerRequest;
import com.markettwin.backend.dto.response.EmergencyAlertDto;
import com.markettwin.backend.dto.response.PedestrianCoordinateDto;
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

    @Value("${ai.secret-key}")
    private String aiSecretKey;

    @GetMapping("/alerts/unresolved")
    public List<EmergencyAlertDto> getUnresolvedAlerts() {
        return controlSystemService.getUnresolvedAlerts();
    }

    @GetMapping("/coordinates-json")
    public List<PedestrianCoordinateDto> getJsonCoordinates(
            @RequestParam Long clipId,
            @RequestParam Integer frameId,
            @RequestParam(required = false) Long videoId) { // Integer -> Long 수정
        return controlSystemService.getJsonCoordinates(clipId, frameId, videoId);
    }

    @PostMapping("/alerts/trigger")
    public ResponseEntity<String> triggerEmergencyAlert(
            @RequestHeader(value = "X-API-KEY", required = false) String apiKey,
            @RequestBody AlertTriggerRequest request) {
        if (apiKey == null || !apiKey.equals(aiSecretKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("❌ 경고: API 키가 일치하지 않거나 누락되었습니다.");
        }
        controlSystemService.triggerAlertFromAi(request.zoneId(), request.alertType());
        return ResponseEntity.ok("✅ 긴급 알람 처리 프로세스가 성공적으로 시작되었습니다.");
    }
}
