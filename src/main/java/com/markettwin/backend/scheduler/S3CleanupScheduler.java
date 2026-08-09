package com.markettwin.backend.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3CleanupScheduler {

    private final S3Client s3Client;

    @Value("${aws.video.bucket}")
    private String bucket;

    // 파이썬이 실시간으로 올리는 1분 단위 영상 폴더 지정 (매우 중요!)
    // 이 폴더(prefix) 하위에 있는 파일만 조회하므로, 다른 팀의 파일이나 risk_clip 폴더는 절대 건드리지 않습니다.
    private static final String ROUTINE_CLIP_PREFIX = "raw-videos/";

    /**
     * 매 10분마다 백그라운드에서 실행 (cron = "0 0/10 * * * *")
     */
    @Scheduled(cron = "0 0/10 * * * *")
    public void cleanupOldRoutineClips() {
        log.info("🧹 [S3 스케줄러] 1시간 경과 raw-videos 청소 시작...");

        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        int deletedCount = 0;

        try {
            // 딱 "raw-video/" 폴더 안에 있는 파일만 검색하도록 제한 (안전장치)
            ListObjectsV2Request listReq = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(ROUTINE_CLIP_PREFIX)
                    .build();

            ListObjectsV2Response listRes = s3Client.listObjectsV2(listReq);

            for (S3Object s3Object : listRes.contents()) {
                if (s3Object.lastModified().isBefore(oneHourAgo)) {
                    DeleteObjectRequest delReq = DeleteObjectRequest.builder()
                            .bucket(bucket)
                            .key(s3Object.key())
                            .build();

                    s3Client.deleteObject(delReq);
                    deletedCount++;
                }
            }
            log.info("🧹 [S3 스케줄러] 청소 완료. 총 {}개 삭제됨.", deletedCount);
        } catch (Exception e) {
            log.error("❌ S3 청소 중 오류 발생: {}", e.getMessage());
        }
    }
}