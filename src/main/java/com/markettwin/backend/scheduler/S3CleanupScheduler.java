package com.markettwin.backend.scheduler;

import com.markettwin.backend.repository.VideoClipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
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
    private final VideoClipRepository videoClipRepository; // 리포지토리 주입 추가

    @Value("${aws.video.bucket}")
    private String bucket;

    // 파이썬이 실시간으로 올리는 1분 단위 영상 폴더 지정 (매우 중요!)
    private static final String ROUTINE_CLIP_PREFIX = "raw-videos/";

    /**
     * 매시 정각마다 백그라운드에서 실행 (cron = "0 0 * * * *")
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional // DB 업데이트를 위해 트랜잭션 추가
    public void cleanupOldRoutineClips() {
        log.info("🧹 [S3 스케줄러] 1시간 경과 raw-videos 청소 및 DB 동기화 시작...");

        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        int s3DeletedCount = 0;

        try {
            // 딱 "raw-video/" 폴더 안에 있는 파일만 검색하도록 제한 (안전장치)
            ListObjectsV2Request listReq = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(ROUTINE_CLIP_PREFIX)
                    .build();

            ListObjectsV2Response listRes = s3Client.listObjectsV2(listReq);

            for (S3Object s3Object : listRes.contents()) {
                // S3 파일의 마지막 수정 시간이 1시간 전보다 과거인 경우 삭제
                if (s3Object.lastModified().isBefore(oneHourAgo)) {
                    DeleteObjectRequest delReq = DeleteObjectRequest.builder()
                            .bucket(bucket)
                            .key(s3Object.key())
                            .build();

                    s3Client.deleteObject(delReq);
                    s3DeletedCount++;
                }
            }

            // S3 삭제 완료 후 DB에도 동일하게 삭제 처리 (is_deleted = true) 동기화
            int dbUpdatedCount = videoClipRepository.markOldRawVideosAsDeleted(oneHourAgo);

            log.info("🧹 [S3 스케줄러] 청소 완료. S3 파일 삭제: {}개, DB 숨김 처리: {}개", s3DeletedCount, dbUpdatedCount);

        } catch (Exception e) {
            log.error("❌ S3 청소 중 오류 발생: {}", e.getMessage());
        }
    }
}
