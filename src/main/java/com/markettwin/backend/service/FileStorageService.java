package com.markettwin.backend.service;

import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.time.Duration;

/**
 * 2026-07-24 추가 (게시판 첨부파일 기능)
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
     * 메모리에 있는 바이트 배열을 업로드한다. (MultipartFile이 아닌 생성 결과물(보고서 업로드용))
     */
    String upload(byte[] content, String contentType, String keyPrefix);

    /**
     * 2026-07-30 추가 (보고서 기능)
     * 보고서 전용 버킷에 업로드하고 키를 반환한다.
     *
     * 게시판 첨부파일과 버킷을 나눈 이유: 사용자 업로드물과 시스템 생성물은 보존 기간과
     * 접근 주체가 달라, 수명주기 규칙과 권한을 각각 걸 수 있게 두는 편이 낫다.
     * 아래 generateReportDownloadUrl과 같은 버킷을 바라봐야 하므로 짝으로 존재한다.
     */
    String uploadReport(byte[] content, String contentType, String keyPrefix);

    /**
     * 2026-07-30 추가 (보고서 기능)
     * 보고서 전용 버킷 객체의 임시 다운로드 URL을 발급한다.
     */
    URL generateReportDownloadUrl(String key, String originalFileName, Duration ttl);

    /**
     * 저장소에서 파일을 삭제한다. 이미 없는 키를 삭제해도 예외를 던지지 않는다(멱등).
     */
    void delete(String key);

    /**
     * 다운로드용 임시 서명 URL을 발급한다. BE가 파일을 직접 스트리밍하지 않고
     * 클라이언트를 이 URL로 리다이렉트시켜 S3에서 바로 받도록 하기 위함.
     */
    URL generatePresignedDownloadUrl(String key, String originalFileName, Duration ttl);
}
