package com.markettwin.backend.dto.response;

import lombok.Builder;
import lombok.Getter;

// 2026-07-24 추가 (게시판 기능)
@Getter
@Builder
public class AttachmentDto {
    private Long attachmentId;
    private String originalName;
    private long fileSize;
    private String contentType;
}
