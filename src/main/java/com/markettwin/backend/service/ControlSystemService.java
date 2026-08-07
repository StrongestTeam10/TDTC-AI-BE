package com.markettwin.backend.service;

import com.markettwin.backend.domain.entity.EmergencyAlert;
import com.markettwin.backend.domain.entity.PostReport;
import com.markettwin.backend.domain.entity.VideoClip;
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
import java.time.Instant;
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
    public void triggerAlertFromAi(AlertTriggerRequest request) {
        if (emergencyAlertRepository.existsByZoneIdAndIsResolvedFalse(request.zoneId())) {
            log.info("구역 {}에 이미 활성화된 신고가 있습니다. 중복 알람을 무시합니다.", request.zoneId());
            return;
        }

        EmergencyAlert alert = EmergencyAlert.builder()
                .zoneId(request.zoneId())
                .alertType(request.alertType() != null ? request.alertType() : "AI_DETECTED")
                .isResolved(false)
                .build();
        emergencyAlertRepository.save(alert);

        if (request.pdfUrl() != null && !request.pdfUrl().isBlank()) {
            PostReport report = PostReport.builder()
                    .alertId(alert.getAlertId())
                    .targetDate(LocalDate.now())
                    .llmSummary(request.llmSummary())
                    .s3PdfUrl(request.pdfUrl())
                    .build();
            postReportRepository.save(report);
        }

        if (request.videoUrl() != null && !request.videoUrl().isBlank()) {
            VideoClip clip = VideoClip.builder()
                    .zoneId(request.zoneId())
                    .clipType(request.alertType() != null ? request.alertType() : "AI_DETECTED")
                    .s3ClipUrl(request.videoUrl())
                    .startTime(Instant.now().minusSeconds(15))
                    .endTime(Instant.now())
                    .expiresAt(Instant.now().plus(Duration.ofDays(30)))
                    .build();
            videoClipRepository.save(clip);
        }

        externalNotificationService.sendEmergencySms(request.zoneId(), request.alertType());
    }
}
