package com.markettwin.backend.service;

import com.markettwin.backend.exception.FileStorageException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

/**
 * 게시판 첨부파일 기능
 * 버킷은 비공개(private)로 두고, 업로드는 BE가 직접 putObject, 다운로드는
 * presigned GET URL을 발급해 클라이언트가 S3에서 바로 받도록 함(BE가 파일을
 * 직접 스트리밍하지 않아 대용량 첨부에도 서버 메모리 부담이 없음).
 */
@Service
@RequiredArgsConstructor
public class S3FileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(S3FileStorageService.class);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucket;

    /**
     * 보고서 저장 위치. 미설정이면 aws.s3.bucket 값을 쓴다.
     *
     * 에 게시판 첨부파일과 버킷을 하나로 합치고 키 prefix(board/ vs reports/)로만
     * 구분하기로 해서, 지금은 위 bucket과 같은 값이 들어온다. 통합이 확정이면 이 설정과
     * 보고서 전용 메서드들을 걷어낼 수 있다.
     */
    @Value("${aws.s3.report-bucket:${aws.s3.bucket}}")
    private String reportBucket;

    @Override
    public String upload(MultipartFile file, String keyPrefix) {
        // 원본 파일명 충돌/특수문자 문제를 피하려고 실제 저장 키는 UUID로 생성하고,
        // 원본 파일명은 brdattc01d.original_name 컬럼에 별도로 보관해서 다운로드 시 복원함
        String key = keyPrefix + "/" + UUID.randomUUID();

        // 수정(보안 감사 BE-14): 업로드 스트림을 try-with-resources로 닫는다.
        // putObject가 스트림을 소비하기는 하지만 닫아주지는 않아서, 게시판 첨부가
        // 여러 건 올라오면 임시 파일 핸들이 남는다.
        try (InputStream in = file.getInputStream()) {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();
            s3Client.putObject(request, RequestBody.fromInputStream(in, file.getSize()));
            return key;
        } catch (IOException | SdkException e) {
            // 기존엔 S3Exception(버킷은 있지만 권한/요청 자체가
            // 거부된 경우)만 잡았는데, AWS 자격증명이 아예 설정 안 돼 있거나(로컬에
            // AWS_ACCESS_KEY_ID/SECRET_ACCESS_KEY 미설정) 버킷 자체가 없는 경우엔
            // SdkClientException(SdkException의 형제뻘 상위 타입)이 발생해서 여기서
            // 못 잡고 그대로 튀어나가 GlobalExceptionHandler의 범용 500으로 빠졌었음.
            // S3Exception의 상위 타입인 SdkException으로 넓혀서 둘 다 잡음.
            log.error("S3 업로드 실패: bucket={}, key={} - AWS 자격증명(AWS_ACCESS_KEY_ID/"
                    + "AWS_SECRET_ACCESS_KEY)과 버킷(aws.s3.bucket={}) 설정을 확인하세요.",
                    bucket, key, bucket, e);
            throw new FileStorageException("파일 업로드에 실패했습니다. S3 설정을 확인해주세요.");
        }
    }

    /**
     * 보고서 기능
     * 보고서 전용 버킷에 올린다. 업로드된 파일이 아니라 BE가 만들어낸 결과물이라
     * MultipartFile이 없어 바이트를 그대로 받는다.
     *
     * 게시판 업로드와 코드를 공통화하지 않은 이유: 이미 검증된 경로를 손대지 않는 편이
     * 안전하고, 두 저장소의 정책(수명주기·권한)이 앞으로 갈라질 수 있어 각자 독립적으로
     * 바뀔 여지를 남겨둔다.
     */
    @Override
    public String uploadReport(byte[] content, String contentType, String keyPrefix) {
        String key = keyPrefix + "/" + UUID.randomUUID();

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(reportBucket)
                    .key(key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(content));
            return key;
        } catch (SdkException e) {
            log.error("S3 보고서 업로드 실패: bucket={}, key={}", reportBucket, key, e);
            throw new FileStorageException("보고서 업로드에 실패했습니다. S3 설정을 확인해주세요.");
        }
    }

    @Override
    public void delete(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
        } catch (SdkException e) {
            // 삭제 실패는 게시글 삭제 자체를 막을 정도의 문제는 아니라고 판단해 로그만 남김
            // S3에 orphan 객체가 남을 수 있음 - 운영 배포 후 별도 정리 배치 고려 가능
            log.error("S3 삭제 실패(무시하고 진행): bucket={}, key={}", bucket, key, e);
        }
    }

    /** delete와 같고 대상만 reportBucket이다. 지금은 두 설정이 같은 버킷을 가리킨다. */
    @Override
    public void deleteReport(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(reportBucket)
                    .key(key)
                    .build());
        } catch (SdkException e) {
            // 이전 보고서 정리는 부가 작업이라, 실패해도 새 보고서 생성 자체는 성공으로 둔다.
            // 남은 객체는 로그로 추적할 수 있게 키를 남긴다.
            log.error("S3 보고서 삭제 실패(무시하고 진행): bucket={}, key={}", reportBucket, key, e);
        }
    }


    @Override
    public URL generatePresignedDownloadUrl(String key, String originalFileName, Duration ttl) {
        // 브라우저가 원본 파일명으로 저장하도록 Content-Disposition을 응답 헤더로 강제.
        // 파일명에 한글/공백이 섞여 있을 수 있어 RFC 5987 형식(filename*=UTF-8''...)으로 인코딩.
        String encodedName = java.net.URLEncoder.encode(originalFileName, StandardCharsets.UTF_8)
                .replace("+", "%20");
        String contentDisposition = "attachment; filename*=UTF-8''" + encodedName;

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .responseContentDisposition(contentDisposition)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url();
    }

    /**
     * 보고서 기능
     * 위 generatePresignedDownloadUrl과 동작이 같고 대상 버킷만 reportBucket이다.
     */
    @Override
    public URL generateReportDownloadUrl(String key, String originalFileName, Duration ttl) {
        String encodedName = java.net.URLEncoder.encode(originalFileName, StandardCharsets.UTF_8)
                .replace("+", "%20");
        String contentDisposition = "attachment; filename*=UTF-8''" + encodedName;

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(reportBucket)
                .key(key)
                .responseContentDisposition(contentDisposition)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url();
    }
}
