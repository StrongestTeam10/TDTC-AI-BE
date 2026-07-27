package com.markettwin.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * BRDPSTS01M - 게시글
 *
 * 2026-07-24 추가 (게시판 기능)
 * 권한 규칙(PostService에서 강제):
 *  - 수정/삭제: 관리자(ROL01) 전체 가능, 그 외는 writerId 본인 글만
 *  - isNotice(공지 고정): 관리자만 true로 설정 가능
 *  - marketCode: 공지는 NULL 허용(시장 무관 항상 노출), 일반 글은 작성자 marketCode로 채움
 */
@Entity
@Table(name = "brdpsts01m")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long postId;

    @Column(name = "market_code", length = 5)
    private String marketCode;

    @Column(name = "writer_id", nullable = false)
    private Long writerId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_notice", nullable = false)
    private boolean notice;

    // 2026-07-24 추가(UI 설계서 반영): 게시판 상단 카테고리 탭(공지사항/자유게시판)
    // 필터 기준. comcode01m BCT 도메인 코드. is_notice(관리자 상단 고정)와는 별개 개념.
    @Column(name = "category_code", nullable = false, length = 5)
    private String categoryCode;

    @Column(name = "view_count", nullable = false)
    private int viewCount;

    @Column(name = "like_count", nullable = false)
    private int likeCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
