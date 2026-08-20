package com.markettwin.backend.service;

import com.markettwin.backend.domain.entity.User;
import com.markettwin.backend.dto.response.PendingUserDto;
import com.markettwin.backend.exception.ForbiddenActionException;
import com.markettwin.backend.exception.UserNotFoundException;
import com.markettwin.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 회원가입 관리자 승인
 *
 * 관리자(ROL01)만 호출 가능. 신규 가입자는 APRPD(대기) 상태로 생성되고
 * (AuthService.signup 참고), 여기서 승인(APRAP)/거부(APRRJ) 처리한다. 거부해도
 * 계정을 삭제하지 않고 상태만 바꿔서 이력을 남긴다.
 */
@Service
@RequiredArgsConstructor
public class UserApprovalService {

    private static final String ADMIN_ROLE_CODE = "ROL01";

    private final UserRepository userRepository;

    public List<PendingUserDto> listPending(User currentUser) {
        assertAdmin(currentUser);
        return userRepository.findByApprovalStatusOrderByCreatedAtAsc("APRPD").stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void approve(Long userId, User currentUser) {
        assertAdmin(currentUser);
        User target = getUserOrThrow(userId);
        target.approve();
    }

    @Transactional
    public void reject(Long userId, User currentUser) {
        assertAdmin(currentUser);
        User target = getUserOrThrow(userId);
        target.reject();
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private void assertAdmin(User user) {
        if (!ADMIN_ROLE_CODE.equals(user.getRulesCode())) {
            throw new ForbiddenActionException("회원 승인 권한이 없습니다.");
        }
    }

    private PendingUserDto toDto(User user) {
        return PendingUserDto.builder()
                .userId(user.getUserId())
                .loginId(user.getLoginId())
                .name(user.getName())
                .phoneNumber(user.getPhoneNumber()) // 08.13 추가
                .orgCode(user.getOrgCode())
                .marketCode(user.getMarketCode())
                .approvalStatus(user.getApprovalStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
