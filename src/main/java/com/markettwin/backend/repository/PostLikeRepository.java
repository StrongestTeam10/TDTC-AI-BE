package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// 2026-07-24 추가 (게시판 기능)
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    Optional<PostLike> findByPostIdAndUserId(Long postId, Long userId);

    boolean existsByPostIdAndUserId(Long postId, Long userId);

    void deleteByPostIdAndUserId(Long postId, Long userId);
}
