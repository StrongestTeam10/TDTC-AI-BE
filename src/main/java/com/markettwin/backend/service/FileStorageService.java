package com.markettwin.backend.service;

import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.time.Duration;

/**
 * 게시판 첨부파일 기능
 * 저장소 구현(S3)을 서비스 레이어에서 직접 의존하지 않도록 인터페이스로 분리.
 * 현재 구현체는 S3FileStorageService 하나뿐이지만, 로컬 디스크 등 다른 저장소로
 * 바꿀 일이 생기면 이 인터페이스만 구현해서 갈아끼울 수 있음.
 */
public interface FileStorageService {

    /**
     * 파일을 업로드하고 저장소 내 키(S3 오브젝트 키)를 반환한다.
     */
    String upload(MultipartFile file, String keyPrefix);

    /**
     * 보고서 기능
     * 보고서를 aws.s3.report-bucket에 업로드하고 키를 반환한다.
     *
     * 업로드된 파일이 아니라 BE가 만들어낸 결과물이라 MultipartFile이 없어 바이트로 받는다.
     * upload()와 따로 있는 1차 이유가 이 입력 타입 차이다.
     *
     * 에 게시판 첨부파일과 버킷을 하나로 합치고 키 prefix(board/ vs reports/)로만
     * 구분하기로 했다. 보고서 3종(uploadReport, generateReportDownloadUrl, deleteReport)은
     * 여전히 aws.s3.report-bucket을 보는데, 통합이 확정이면 함께 정리할 수 있다.
     */
    String uploadReport(byte[] content, String contentType, String keyPrefix);

    /**
     * 보고서 기능
     * 보고서 전용 버킷 객체의 임시 다운로드 URL을 발급한다.
     */
    URL generateReportDownloadUrl(String key, String originalFileName, Duration ttl);

    /**
     * 저장소에서 파일을 삭제한다. 이미 없는 키를 삭제해도 예외를 던지지 않는다(멱등).
     */
    void delete(String key);

    /**
     * 보고서 기능
     * 보고서 객체를 삭제한다.
     *
     * 보고서 업로드·다운로드가 aws.s3.report-bucket을 보므로 삭제도 같은 설정을 따르게 했다.
     * 에 게시판 첨부파일과 버킷을 하나로 합쳐서, 지금은 delete()와 결과가 같다.
     */
    void deleteReport(String key);

    /**
     * 다운로드용 임시 서명 URL을 발급한다. BE가 파일을 직접 스트리밍하지 않고
     * 클라이언트를 이 URL로 리다이렉트시켜 S3에서 바로 받도록 하기 위함.
     */
    URL generatePresignedDownloadUrl(String key, String originalFileName, Duration ttl);
}
