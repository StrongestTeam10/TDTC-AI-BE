package com.markettwin.backend.service;

import com.markettwin.backend.domain.entity.User;
import com.markettwin.backend.dto.response.UserSummaryDto;
import com.markettwin.backend.exception.ForbiddenActionException;
import com.markettwin.backend.exception.InvalidRoleCodeException;
import com.markettwin.backend.exception.UserNotFoundException;
import com.markettwin.backend.repository.CommonCodeRepository;
import com.markettwin.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * 2026-08-05 추가 (회원관리)
 *
 * 권한 규칙: 목록 조회/권한 변경 둘 다 관리자(ROL01) 전용. PostService/MarketService와
 * 동일하게 URL 단위가 아니라 서비스 레이어에서 rulesCode 직접 비교로 강제함
 * (SecurityConfig는 인증 여부만 확인 - "로그인은 했지만 관리자가 아님"은 여기서 403).
 *
 * 화면 구성(재재님 요청): 게시판의 "관리자 시장 탭"과 동일한 로직으로 좌상단에서
 * 시장을 고르고, "사용자 관리"(전체) / "회원 승인"(rules_code=ROL03, 즉 가입 후
 * 아직 관리자가 권한을 지정해주지 않은 기본값 그대로인 회원만) 두 탭으로 나눔.
 *
 * 권한 변경은 즉시 반영, 별도 이력 기록 없음(재재님 확인) - User.updatedAt/updatedIp에는
 * 남으므로 "마지막으로 언제 바뀌었는지" 정도는 엔티티에 이미 있는 컬럼으로 커버됨.
 */
@Service
@RequiredArgsConstructor
public class UserAdminService {

    private static final String ADMIN_ROLE_CODE = "ROL01";
    private static final String PENDING_ROLE_CODE = "ROL03"; // 회원가입 시 기본 부여되는 권한(=미검토 상태로 간주)
    private static final String ROLE_CODE_COB = "ROL"; // comcode01m.code_cob

    private final UserRepository userRepository;
    private final CommonCodeRepository commonCodeRepository;

    public List<UserSummaryDto> list(String marketCode, boolean pendingOnly, User currentUser) {
        requireAdmin(currentUser);

        boolean hasMarket = marketCode != null && !marketCode.isBlank();
        List<User> users;
        if (hasMarket && pendingOnly) {
            users = userRepository.findByMarketCodeAndRulesCodeOrderByCreatedAtDesc(marketCode, PENDING_ROLE_CODE);
        } else if (hasMarket) {
            users = userRepository.findByMarketCodeOrderByCreatedAtDesc(marketCode);
        } else if (pendingOnly) {
            users = userRepository.findByRulesCodeOrderByCreatedAtDesc(PENDING_ROLE_CODE);
        } else {
            users = userRepository.findAllByOrderByCreatedAtDesc();
        }

        return users.stream().map(this::toSummary).toList();
    }

    @Transactional
    public UserSummaryDto updateRole(Long targetUserId, String newRulesCode, User currentUser, String clientIp) {
        requireAdmin(currentUser);
        validateRoleCode(newRulesCode);

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException(targetUserId));

        // 마지막 관리자를 강등시키면 아무도 이 화면에 다시 못 들어오게 되므로 방지
        boolean isDemotingLastAdmin = ADMIN_ROLE_CODE.equals(target.getRulesCode())
                && !ADMIN_ROLE_CODE.equals(newRulesCode)
                && userRepository.countByRulesCode(ADMIN_ROLE_CODE) <= 1;
        if (isDemotingLastAdmin) {
            throw new ForbiddenActionException("마지막 남은 관리자(ROL01)의 권한은 해제할 수 없습니다.");
        }

        target.setRulesCode(newRulesCode);
        target.setUpdatedAt(Instant.now());
        target.setUpdatedIp(clientIp);

        return toSummary(target);
    }

    private void requireAdmin(User user) {
        if (!ADMIN_ROLE_CODE.equals(user.getRulesCode())) {
            throw new ForbiddenActionException("관리자만 이용할 수 있습니다.");
        }
    }

    private void validateRoleCode(String rulesCode) {
        boolean valid = rulesCode != null
                && commonCodeRepository.existsByCodeCobAndCode(ROLE_CODE_COB, rulesCode);
        if (!valid) {
            throw new InvalidRoleCodeException(rulesCode);
        }
    }

    private UserSummaryDto toSummary(User user) {
        return UserSummaryDto.builder()
                .userId(user.getUserId())
                .loginId(user.getLoginId())
                .name(user.getName())
                .rulesCode(user.getRulesCode())
                .orgCode(user.getOrgCode())
                .marketCode(user.getMarketCode())
                .build();
    }
}
