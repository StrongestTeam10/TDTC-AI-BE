package com.markettwin.backend.scheduler;

import com.markettwin.backend.repository.EmergencyAlertRepository;
import com.markettwin.backend.repository.ExternalFactorRepository;
import com.markettwin.backend.repository.PedestrianCoordinateJsonRepository;
import com.markettwin.backend.repository.PostReportRepository;
import com.markettwin.backend.repository.RiskRepository;
import com.markettwin.backend.repository.VideoClipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiDataCleanupScheduler {

    private final RiskRepository riskRepository;
    private final PedestrianCoordinateJsonRepository pedestrianCoordinateRepository;
    private final VideoClipRepository videoClipRepository;

    private final ExternalFactorRepository externalFactorRepository;
    private final PostReportRepository postReportRepository;
    private final EmergencyAlertRepository emergencyAlertRepository;

    /**
     * [트랙 A] 용량 폭탄 청소반 (매시 정각 1시간마다 실행)
     * 대상: 1시간 지난 무거운 데이터 (점수, 좌표, TEMP 일반 영상)
     * 외래키 순서: 증손자(Risk) -> 손자(Pedestrian) -> 자식(VideoClip)
     */
    @Transactional
    @Scheduled(cron = "0 0 * * * *")
    public void cleanUpHeavyDataHourly() {
        log.info("🧹 [트랙 A] 1시간 주기 무거운 데이터 청소 시작...");
        try {
            Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);

            // 1. 가장 막내 자식 삭제 (위험 점수)
            riskRepository.deleteByDetectedAtBefore(oneHourAgo);

            // 2. 그 위 자식 삭제 (보행자 좌표)
            pedestrianCoordinateRepository.deleteByCapturedAtBefore(oneHourAgo);

            // 3. 부모 영상 삭제 (★ TEMP만 삭제, RISK 영상은 절대 보존)
            videoClipRepository.deleteTempClipsOlderThan(oneHourAgo);

            log.info("✅ [트랙 A] 1시간 주기 청소 완료");
        } catch (Exception e) {
            log.error("❌ [트랙 A] 청소 중 에러 발생: {}", e.getMessage());
        }
    }

    /**
     * [트랙 B] 사건 기록 청소반 (매일 새벽 2시, 24시간 주기로 실행)
     * 대상: 24시간 지난 과거 하루치 기록 (보고서, 신고 이력, 외부 요인)
     * 외래키 순서: 자식(보고서) -> 부모(신고 이력) -> 독립(외부 요인)
     */
    @Transactional
    @Scheduled(cron = "0 0 2 * * *") // 매일 02:00:00 실행
    public void cleanUpLogDataDaily() {
        log.info("🧹 [트랙 B] 24시간 주기 과거 사건 기록 청소 시작...");
        try {
            Instant oneDayAgo = Instant.now().minus(24, ChronoUnit.HOURS);

            // 1. 자식 보고서 삭제
            postReportRepository.deleteByCreatedAtBefore(oneDayAgo);

            // 2. 부모 신고 이력 삭제
            emergencyAlertRepository.deleteByAlertedAtBefore(oneDayAgo);

            // 3. 부모 외부 요인 삭제 (독립 테이블)
            externalFactorRepository.deleteByUpdatedAtBefore(oneDayAgo);

            log.info("✅ [트랙 B] 24시간 주기 청소 완료");
        } catch (Exception e) {
            log.error("❌ [트랙 B] 청소 중 에러 발생: {}", e.getMessage());
        }
    }
}
