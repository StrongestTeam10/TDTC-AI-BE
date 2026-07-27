package com.markettwin.backend.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * 2026-07-24 추가 (게시판 기능)
 * canEdit/canDelete/liked는 "현재 요청한 사용자" 기준으로 서버가 미리 계산해서 내려줌
 * - FE가 role/writerId 비교 로직을 따로 들고 있지 않아도 되게 하기 위함
 */
@Getter
@Builder
public class PostDetailDto {
    private Long postId;
    private String title;
    private String content;
    private Long writerId;
    private String writerName;
    private String marketCode;
    private String categoryCode;
    private boolean notice;
    private int viewCount;
    private int likeCount;
    private boolean liked;
    private boolean canEdit;
    private boolean canDelete;
    private List<AttachmentDto> attachments;
    private Instant createdAt;
    private Instant updatedAt;
}
