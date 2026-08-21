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
import java.util.List;

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

    @Transactional
    @Scheduled(cron = "0 0 * * * *")
    public void cleanUpHeavyDataHourly() {
        log.info("🧹 [트랙 A] 1시간 주기 무거운 데이터 청소 시작...");
        try {
            Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);

            List<Long> clipIds = videoClipRepository.findTempClipIdsOlderThan(oneHourAgo);

            if (!clipIds.isEmpty()) {
                riskRepository.deleteByVideoClipIds(clipIds);
                pedestrianCoordinateRepository.deleteByClipIdsIn(clipIds);
                videoClipRepository.deleteByClipIdsIn(clipIds);
            }

            log.info("✅ [트랙 A] 청소 완료 (삭제된 영상 클립 수: {})", clipIds.size());
        } catch (Exception e) {
            log.error("❌ [트랙 A] 청소 중 에러 발생: {}", e.getMessage(), e);
        }
    }


    @Transactional
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanUpLogDataDaily() {
        log.info("🧹 [트랙 B] 24시간 주기 과거 사건 기록 청소 시작...");
        try {
            Instant oneDayAgo = Instant.now().minus(24, ChronoUnit.HOURS);

            // 1. 자식 보고서 삭제
            postReportRepository.deleteByCreatedAtBefore(oneDayAgo);

            // 2. 부모 신고 이력 삭제
            emergencyAlertRepository.deleteByAlertedAtBefore(oneDayAgo);

            // 3. 외부 요인 삭제 (참조되지 않는 것만 안전하게)
            externalFactorRepository.deleteUnreferencedOlderThan(oneDayAgo);

            log.info("✅ [트랙 B] 24시간 주기 청소 완료");
        } catch (Exception e) {
            log.error("❌ [트랙 B] 청소 중 에러 발생: {}", e.getMessage(), e);
        }
    }
}
