package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLoginId(String loginId);

    // 2026-07-24 추가: 회원가입 시 아이디 중복 확인용
    boolean existsByLoginId(String loginId);

    // 2026-07-26 추가: 게시판 목록 조회 N+1 해결용 — 작성자 ID 목록을 받아
    // (userId, name)만 한 번의 쿼리로 일괄 조회 (User 엔티티 전체를 안 불러옴).
    @Query("SELECT u.userId, u.name FROM User u WHERE u.userId IN :userIds")
    List<Object[]> findNamesByIds(@Param("userIds") List<Long> userIds);
}
