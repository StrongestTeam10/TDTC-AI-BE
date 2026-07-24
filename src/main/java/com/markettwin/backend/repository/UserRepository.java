package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLoginId(String loginId);

    // 2026-07-24 추가: 회원가입 시 아이디 중복 확인용
    boolean existsByLoginId(String loginId);
}
