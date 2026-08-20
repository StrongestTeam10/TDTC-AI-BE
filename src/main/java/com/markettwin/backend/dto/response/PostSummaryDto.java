package com.markettwin.backend.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

// (게시판 기능) - 목록(리스트/공지 고정 영역)에서 쓰는 요약 응답
@Getter
@Builder
public class PostSummaryDto {
    private Long postId;
    private String title;
    private String writerName;
    private String marketCode;
    private String categoryCode;
    private boolean notice;
    private int viewCount;
    private int likeCount;
    private int attachmentCount;
    private Instant createdAt;
}
