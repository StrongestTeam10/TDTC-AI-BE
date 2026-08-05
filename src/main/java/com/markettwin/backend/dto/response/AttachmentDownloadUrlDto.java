package com.markettwin.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 2026-08-04 추가
 *
 * 첨부파일 다운로드 API가 302 리다이렉트 대신 이 DTO로 presigned URL을 반환한다.
 * 크로스오리진 302 리다이렉트는 브라우저가 리다이렉트 대상 요청의 Origin 헤더를
 * "null"로 바꿔버려서(표준 스펙 동작) S3 CORS 설정이 절대 통과할 수 없는 구조적
 * 문제가 있었음 - FE가 이 URL로 window.location을 통해 직접 이동(진짜 페이지 이동은
 * CORS 검사 대상이 아님)하는 방식으로 우회함.
 */
@Getter
@AllArgsConstructor
public class AttachmentDownloadUrlDto {
    private String downloadUrl;
}
