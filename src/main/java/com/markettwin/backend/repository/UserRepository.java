package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLoginId(String loginId);

    // 회원가입 시 아이디 중복 확인용
    boolean existsByLoginId(String loginId);

    // 게시판 목록 조회 N+1 해결용 — 작성자 ID 목록을 받아
    // (userId, name)만 한 번의 쿼리로 일괄 조회 (User 엔티티 전체를 안 불러옴).
    @Query("SELECT u.userId, u.name FROM User u WHERE u.userId IN :userIds")
    List<Object[]> findNamesByIds(@Param("userIds") List<Long> userIds);
    // (비밀번호 찾기): 아이디+이름+소속기관+담당시장이 모두 일치하는
    // 계정을 찾는다. verifyIdentity(존재 여부만 확인)와 resetPassword(재검증 후 변경)
    // 둘 다 이 메서드를 씀 - 두 시점에 서로 다른 조건으로 어긋나지 않도록 통일.
    Optional<User> findByLoginIdAndNameAndOrgCodeAndMarketCode(
            String loginId, String name, String orgCode, String marketCode);

    // (회원가입 관리자 승인): UserApprovalService.listPending()에서
    // 승인 대기(APRPD) 계정을 오래된 순으로 보여주는 용도.
    List<User> findByApprovalStatusOrderByCreatedAtAsc(String approvalStatus);

    // 회원관리 - 사용자 관리/회원 승인 화면
    // 게시판의 "관리자 시장 탭"과 동일한 로직: marketCode 없으면 전체, 있으면 해당
    // 시장만. pendingOnly 여부에 따라 rulesCode='ROL03'(기본 가입 권한, 아직 관리자가
    // 검토 전) 필터를 추가로 거는 4가지 조합을 각각의 derived query로 구성함.
    List<User> findAllByOrderByCreatedAtDesc();

    List<User> findByRulesCodeOrderByCreatedAtDesc(String rulesCode);

    List<User> findByMarketCodeOrderByCreatedAtDesc(String marketCode);

    List<User> findByMarketCodeAndRulesCodeOrderByCreatedAtDesc(String marketCode, String rulesCode);

    // 권한 변경 시 "마지막 관리자(ROL01)를 강등시키는" 상황을
    // 막기 위한 안전장치(UserAdminService.updateRole 참고).
    long countByRulesCode(String rulesCode);

    // 특정 시장의 당직자만 조회
    List<User> findByMarketCodeAndIsDutyTrue(String marketCode);
}
