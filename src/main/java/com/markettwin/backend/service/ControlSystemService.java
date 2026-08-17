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
import com.markettwin.backend.repository.RiskRepository;
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

    // 🟢 새로 추가된 서비스들
    private final RiskRepository riskRepository;
    private final PdfReportService pdfReportService;

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

    // 🟢 완전히 업그레이드된 신고 발동 메서드
    @Transactional
    public Long triggerAlertFromAi(AlertTriggerRequest request) {
        return emergencyAlertRepository.findFirstByZoneIdAndIsResolvedFalse(request.zoneId())
                .map(EmergencyAlert::getAlertId)
                .orElseGet(() -> {
                    // 1. 알람 DB 저장 (기존과 동일)
                    EmergencyAlert alert = EmergencyAlert.builder()
                            .zoneId(request.zoneId())
                            .alertType(request.alertType() != null ? request.alertType() : "AI_DETECTED")
                            .isResolved(false)
                            .build();
                    emergencyAlertRepository.save(alert);

                    // 2. 최신 위험 데이터(4가지 수치) 조회
                    com.markettwin.backend.domain.entity.Risk latestRisk = riskRepository.findLatestRiskByZoneId(request.zoneId()).orElse(null);


                    if (latestRisk != null) {
                        // 3. PDF 생성
                        byte[] pdfBytes = pdfReportService.generateEmergencyReport(request.zoneId(), latestRisk);

                        // 4. S3 업로드 (VideoS3Service 사용 -> tdtc-cctv-upload 버킷의 post-reports 폴더로 들어감)
                        String s3Key = videoS3Service.uploadCctvReport(pdfBytes, "application/pdf", "post-reports");

                        // 5. PostReport DB 테이블에 S3 주소(Key) 적재
                        com.markettwin.backend.domain.entity.PostReport postReport = com.markettwin.backend.domain.entity.PostReport.builder()
                                .alertId(alert.getAlertId())
                                .targetDate(LocalDate.now())
                                .s3PdfUrl(s3Key)
                                .build();
                        postReportRepository.save(postReport);
                    }

                    // 6. 문자 발송 (기존과 동일)
                    externalNotificationService.sendEmergencySms(request.zoneId(), request.alertType());

                    return alert.getAlertId();
                });
    }

    @Transactional
    public void resolveAlert(Long alertId) {
        EmergencyAlert alert = emergencyAlertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("해당 알람을 찾을 수 없습니다: " + alertId));
        alert.resolve();
    }
}
