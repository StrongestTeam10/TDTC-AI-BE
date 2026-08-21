package com.markettwin.backend.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 게시판 기능
 * pinned: 공지(is_notice=true) 전체, 페이징 없이 항상 상단 고정 노출
 * page: 공지를 제외한 일반 게시글의 페이지 결과
 */
@Getter
@Builder
public class PostListResponseDto {
    private List<PostSummaryDto> pinned;
    private PageResponseDto<PostSummaryDto> page;
}
