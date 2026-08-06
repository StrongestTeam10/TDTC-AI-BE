package com.markettwin.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;
import java.time.Duration;

@Service
public class VideoS3Service {

    private final S3Presigner s3Presigner;
    private final String bucket;

    public VideoS3Service(S3Presigner s3Presigner,
                          @Value("${aws.video.bucket}") String bucket) {
        this.s3Presigner = s3Presigner;
        this.bucket = bucket;
    }

    public URL generatePresignedUploadUrl(String key, String contentType, Duration ttl) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .putObjectRequest(putObjectRequest)
                .build();

        return s3Presigner.presignPutObject(presignRequest).url();
    }

    public URL generatePresignedDownloadUrl(String keyOrUrl, Duration ttl) {
        String key = keyOrUrl;

        if (key != null && key.startsWith("http")) {
            try {
                java.net.URL url = new java.net.URL(key);
                key = url.getPath();
                if (key.startsWith("/")) {
                    key = key.substring(1);
                }
                if (key.startsWith(bucket + "/")) {
                    key = key.substring(bucket.length() + 1);
                }
            } catch (Exception e) {
            }
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url();
    }

}
