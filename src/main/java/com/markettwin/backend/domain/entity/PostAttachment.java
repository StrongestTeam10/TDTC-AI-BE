package com.markettwin.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * BRDATTC01D - 게시글 첨부파일
 *
 * 2026-07-24 추가 (게시판 기능)
 * 파일 바이너리는 S3에 저장하고, 여기는 메타데이터 + S3 오브젝트 키(s3Key)만 보관.
 */
@Entity
@Table(name = "brdattc01d")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attachment_id")
    private Long attachmentId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "s3_key", nullable = false, length = 500)
    private String s3Key;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
