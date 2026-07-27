package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.PostAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

// 2026-07-24 추가 (게시판 기능)
public interface PostAttachmentRepository extends JpaRepository<PostAttachment, Long> {

    List<PostAttachment> findByPostId(Long postId);

    // 2026-07-26 추가: 목록 조회 N+1 해결용 — 게시글 ID 목록을 받아 첨부파일 개수를
    // 한 번의 쿼리(GROUP BY)로 일괄 조회. 결과는 [postId, count] 형태의 Object[] 리스트.
    @Query("SELECT a.postId, COUNT(a) FROM PostAttachment a WHERE a.postId IN :postIds GROUP BY a.postId")
    List<Object[]> countByPostIdIn(@Param("postIds") List<Long> postIds);
}
