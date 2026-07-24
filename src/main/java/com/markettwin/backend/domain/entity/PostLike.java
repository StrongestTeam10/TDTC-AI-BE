package com.markettwin.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * BRDLIKE01D - 게시글 좋아요
 *
 * 2026-07-24 추가 (게시판 기능)
 * (post_id, user_id) UNIQUE 제약으로 중복 좋아요 방지 (schema-init.sql 참고)
 */
@Entity
@Table(name = "brdlike01d")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "like_id")
    private Long likeId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
