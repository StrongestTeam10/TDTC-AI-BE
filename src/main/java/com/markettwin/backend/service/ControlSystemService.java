package com.markettwin.backend.service;

import com.markettwin.backend.domain.entity.EmergencyAlert;
import com.markettwin.backend.domain.entity.PedestrianCoordinateJson;
import com.markettwin.backend.domain.entity.PostReport;
import com.markettwin.backend.repository.EmergencyAlertRepository;
import com.markettwin.backend.repository.PedestrianCoordinateJsonRepository;
import com.markettwin.backend.repository.PostReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 👈 log 에러를 해결해주는 마법의 임포트!
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ControlSystemService {

    private final EmergencyAlertRepository emergencyAlertRepository;
    private final PostReportRepository postReportRepository;
    private final PedestrianCoordinateJsonRepository jsonCoordRepository;

    // 비동기 알림 서비스 주입
    private final ExternalNotificationService externalNotificationService;

    // 미해결 긴급 신고 목록 조회
    public List<EmergencyAlert> getUnresolvedAlerts() {
        return emergencyAlertRepository.findByIsResolvedFalse();
    }

    // 날짜 및 비디오 ID로 사후 분석 보고서 조회
    public List<PostReport> getReportsByDate(LocalDate date, Integer videoId) {
        return postReportRepository.findByTargetDateAndVideoId(date, videoId);
    }

    // 보행자 좌표 리스트 조회
    public List<PedestrianCoordinateJson> getJsonCoordinates(Long clipId, Integer frameId, Integer videoId) {
        return jsonCoordRepository.findByClipIdAndFrameIdAndVideoId(clipId, frameId, videoId);
    }

    // 🚨 AI(파이썬)에서 위험 감지 시 호출되는 자동 신고 로직
    @Transactional
    public void triggerAlertFromAi(Long zoneId, String alertType) {
        // 1. 이미 출동(신고) 중인 구역이면 파이썬이 계속 찔러도 무시 (중복 폭탄 방지)
        if (emergencyAlertRepository.existsByZoneIdAndIsResolvedFalse(zoneId)) {
            log.info("구역 {}에 이미 활성화된 신고가 있습니다. 중복 알람을 무시합니다.", zoneId);
            return;
        }

        // 2. DB에 긴급 신고 기록 남기기
        EmergencyAlert alert = EmergencyAlert.builder()
                .zoneId(zoneId)
                .alertType(alertType != null ? alertType : "AI_DETECTED")
                .isResolved(false)
                .build();
        emergencyAlertRepository.save(alert);

        externalNotificationService.sendEmergencySms(zoneId, alertType);
    }
}
