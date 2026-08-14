package com.markettwin.backend.service;

import com.markettwin.backend.domain.entity.EmergencyAlert;
import com.markettwin.backend.dto.request.AlertTriggerRequest;
import com.markettwin.backend.dto.response.EmergencyAlertDto;
import com.markettwin.backend.dto.response.PedestrianCoordinateDto;
import com.markettwin.backend.dto.response.PostReportDto;
import com.markettwin.backend.repository.EmergencyAlertRepository;
import com.markettwin.backend.repository.PedestrianCoordinateJsonRepository;
import com.markettwin.backend.repository.PostReportRepository;
import com.markettwin.backend.repository.VideoClipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ControlSystemService {

    private final EmergencyAlertRepository emergencyAlertRepository;
    private final PostReportRepository postReportRepository;
    private final VideoClipRepository videoClipRepository;
    private final PedestrianCoordinateJsonRepository jsonCoordRepository;
    private final ExternalNotificationService externalNotificationService;
    private final VideoS3Service videoS3Service;

    public List<EmergencyAlertDto> getUnresolvedAlerts() {
        return emergencyAlertRepository.findByIsResolvedFalse().stream()
                .map(EmergencyAlertDto::from).toList();
    }

    public List<PostReportDto> getReportsByDate(LocalDate date, Long videoId) {
        return postReportRepository.findByTargetDateAndVideoId(date, videoId).stream()
                .map(report -> {
                    String viewUrl = (report.getS3PdfUrl() != null && !report.getS3PdfUrl().isBlank())
                            ? videoS3Service.generatePresignedDownloadUrl(report.getS3PdfUrl(), Duration.ofHours(1)).toString()
                            : null;
                    return PostReportDto.from(report, viewUrl);
                }).toList();
    }

    public List<PedestrianCoordinateDto> getJsonCoordinates(Long clipId, Integer frameId, Long videoId) {
        return jsonCoordRepository.findByClipIdAndFrameIdAndVideoId(clipId, frameId, videoId).stream()
                .map(PedestrianCoordinateDto::from).toList();
    }

    @Transactional
    public Long triggerAlertFromAi(AlertTriggerRequest request) {
        // 이미 진행 중인 알람이 있으면 기존 ID 반환, 없으면 새로 만들고 SMS 발송 후 ID 반환
        return emergencyAlertRepository.findFirstByZoneIdAndIsResolvedFalse(request.zoneId())
                .map(EmergencyAlert::getAlertId)
                .orElseGet(() -> {
                    EmergencyAlert alert = EmergencyAlert.builder()
                            .zoneId(request.zoneId())
                            .alertType(request.alertType() != null ? request.alertType() : "AI_DETECTED")
                            .isResolved(false)
                            .build();
                    emergencyAlertRepository.save(alert);

                    externalNotificationService.sendEmergencySms(request.zoneId(), request.alertType());

                    return alert.getAlertId();
                });
    }

    // TDTC-AI-BE/src/main/java/com/markettwin/backend/service/ControlSystemService.java

    @Transactional
    public void resolveAlert(Long alertId) {
        EmergencyAlert alert = emergencyAlertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("해당 알람을 찾을 수 없습니다: " + alertId));
        alert.resolve();
    }

}
