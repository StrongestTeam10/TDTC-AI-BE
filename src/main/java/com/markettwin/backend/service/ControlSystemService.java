package com.markettwin.backend.service;

import com.markettwin.backend.domain.entity.EmergencyAlert;
import com.markettwin.backend.dto.response.EmergencyAlertDto;
import com.markettwin.backend.dto.response.PedestrianCoordinateDto;
import com.markettwin.backend.dto.response.PostReportDto;
import com.markettwin.backend.repository.EmergencyAlertRepository;
import com.markettwin.backend.repository.PedestrianCoordinateJsonRepository;
import com.markettwin.backend.repository.PostReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final ExternalNotificationService externalNotificationService;

    public List<EmergencyAlertDto> getUnresolvedAlerts() {
        return emergencyAlertRepository.findByIsResolvedFalse().stream()
                .map(EmergencyAlertDto::from).toList();
    }

    public List<PostReportDto> getReportsByDate(LocalDate date, Long videoId) { // Integer -> Long 수정
        return postReportRepository.findByTargetDateAndVideoId(date, videoId).stream()
                .map(PostReportDto::from).toList();
    }

    public List<PedestrianCoordinateDto> getJsonCoordinates(Long clipId, Integer frameId, Long videoId) { // Integer -> Long 수정
        return jsonCoordRepository.findByClipIdAndFrameIdAndVideoId(clipId, frameId, videoId).stream()
                .map(PedestrianCoordinateDto::from).toList();
    }

    @Transactional
    public void triggerAlertFromAi(Long zoneId, String alertType) {
        if (emergencyAlertRepository.existsByZoneIdAndIsResolvedFalse(zoneId)) {
            log.info("구역 {}에 이미 활성화된 신고가 있습니다. 중복 알람을 무시합니다.", zoneId);
            return;
        }
        EmergencyAlert alert = EmergencyAlert.builder()
                .zoneId(zoneId)
                .alertType(alertType != null ? alertType : "AI_DETECTED")
                .isResolved(false)
                .build();
        emergencyAlertRepository.save(alert);
        externalNotificationService.sendEmergencySms(zoneId, alertType);
    }
}
