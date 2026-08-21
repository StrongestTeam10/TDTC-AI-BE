package com.markettwin.backend.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

// (게시판 기능) - Spring Data Page를 그대로 노출하지 않고
// FE에서 쓰기 쉬운 형태로 얇게 감쌈
@Getter
@Builder
public class PageResponseDto<T> {
    private List<T> content;
    private int page;       // 0-base
    private int size;
    private long totalElements;
    private int totalPages;

    public static <T> PageResponseDto<T> from(Page<T> page) {
        return PageResponseDto.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}
