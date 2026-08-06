package com.markettwin.backend.controller;

import com.markettwin.backend.service.VideoS3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/routine-videos")
@RequiredArgsConstructor
public class RoutineVideoController {

    private final S3Client s3Client;
    private final VideoS3Service videoS3Service;

    @Value("${aws.video.bucket}")
    private String bucket;

    private static final String ROUTINE_CLIP_PREFIX = "raw-videos/";

    @GetMapping
    public List<Map<String, String>> getRoutineVideos() {
        List<Map<String, String>> videoList = new ArrayList<>();

        try {
            ListObjectsV2Request listReq = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(ROUTINE_CLIP_PREFIX)
                    .build();

            ListObjectsV2Response listRes = s3Client.listObjectsV2(listReq);

            for (S3Object s3Object : listRes.contents()) {
                if (s3Object.key().equals(ROUTINE_CLIP_PREFIX)) continue;

                String downloadUrl = videoS3Service.generatePresignedDownloadUrl(s3Object.key(), Duration.ofHours(1)).toString();
                String fileName = s3Object.key().replace(ROUTINE_CLIP_PREFIX, "");

                Map<String, String> videoData = new HashMap<>();
                videoData.put("fileName", fileName);
                videoData.put("downloadUrl", downloadUrl);
                videoData.put("lastModified", s3Object.lastModified().toString());

                videoList.add(videoData);
            }
        } catch (Exception e) {
            log.error("❌ S3에서 1분 단위 상시 녹화 영상 목록을 가져오는 중 오류 발생: {}", e.getMessage());
        }

        return videoList;
    }
}
