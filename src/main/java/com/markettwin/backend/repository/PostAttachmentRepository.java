package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.PostAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// 2026-07-24 추가 (게시판 기능)
public interface PostAttachmentRepository extends JpaRepository<PostAttachment, Long> {

    List<PostAttachment> findByPostId(Long postId);
}
