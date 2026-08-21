package com.markettwin.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.core.sync.RequestBody;
import java.util.UUID;

import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
@Service
public class VideoS3Service {

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;
    private final String bucket;

    public VideoS3Service(S3Presigner s3Presigner, S3Client s3Client,
                          @Value("${aws.video.bucket}") String bucket) {
        this.s3Presigner = s3Presigner;
        this.s3Client = s3Client;
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

        // 이미 완성된 S3 주소가 들어오면 거기서 객체 키만 뽑아 쓴다(호출부가 URL을
        // 그대로 넘기는 경우가 있어 둘 다 받아준다).
        if (key != null && key.startsWith("http")) {
            try {
                // new URL(String)은 JDK 20부터 deprecated다(JDK-8294241).
                // 검증 없이 관대하게 파싱해서 라이브러리마다 결과가 갈리는 문제가 있어
                // URI 경유로 바꾼다. 예외 종류가 MalformedURLException ->
                // IllegalArgumentException으로 바뀌지만, 아래에서 Exception으로 함께 받는다.
                URL url = URI.create(key).toURL();
                // getPath()는 퍼센트 인코딩된 상태로 나온다(예: 한글·공백 파일명).
                // S3 객체 키는 디코딩된 원문이라 되돌려야 실제 키와 맞는다.
                String path = URLDecoder.decode(url.getPath(), StandardCharsets.UTF_8);
                if (path.startsWith("/")) {
                    path = path.substring(1);
                }
                // path-style 주소(https://s3.../{bucket}/{key})면 버킷 이름이 앞에 붙어 온다.
                if (path.startsWith(bucket + "/")) {
                    path = path.substring(bucket.length() + 1);
                }
                key = path;
            } catch (Exception e) {
                // 원래 빈 catch였다. 파싱이 실패하면 원본 문자열을 그대로 키로
                // 쓰게 되는데, 그러면 엉뚱한 키로 presigned URL이 만들어지고도 아무 흔적이
                // 남지 않아 원인을 찾을 수 없었다. 동작(원본 유지)은 그대로 두고 기록만 남긴다.
                log.warn("S3 주소에서 객체 키를 추출하지 못해 입력값을 그대로 사용합니다: {}", keyOrUrl, e);
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

    public String uploadCctvReport(byte[] content, String contentType, String keyPrefix) {
        String key = keyPrefix + "/" + UUID.randomUUID() + ".pdf";
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket) // tdtc-cctv-upload 버킷 사용!
                .key(key)
                .contentType(contentType)
                .build();
        s3Client.putObject(request, RequestBody.fromBytes(content));
        return key;
    }

}
