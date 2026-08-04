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
    // 2026-08-04 추가 (비밀번호 찾기): 아이디+이름+소속기관+담당시장이 모두 일치하는
    // 계정을 찾는다. verifyIdentity(존재 여부만 확인)와 resetPassword(재검증 후 변경)
    // 둘 다 이 메서드를 씀 - 두 시점에 서로 다른 조건으로 어긋나지 않도록 통일.
    Optional<User> findByLoginIdAndNameAndOrgCodeAndMarketCode(
            String loginId, String name, String orgCode, String marketCode);
}
