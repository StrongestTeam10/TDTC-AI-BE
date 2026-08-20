package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * 게시판 기능
 * JpaSpecificationExecutor: 목록 조회 시 "시장 범위 + 공지 제외 + 검색어" 조건을
 * 동적으로 조합해야 해서(관리자/일반 사용자별로 조건이 달라짐) Specification 방식 사용.
 * findAll(Specification, Pageable)은 JpaSpecificationExecutor가 이미 제공함.
 * PostService.PostSpecs 참고.
 */
public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

    // 상단 고정 공지 목록 - 시장 무관 항상 전체 노출, 페이징 없이 최신순
    List<Post> findByNoticeTrueOrderByCreatedAtDesc();
}
