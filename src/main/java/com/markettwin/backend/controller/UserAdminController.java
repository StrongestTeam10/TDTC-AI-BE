package com.markettwin.backend.controller;

import com.markettwin.backend.domain.entity.User;
import com.markettwin.backend.dto.request.UpdateUserRoleRequestDto;
import com.markettwin.backend.dto.response.UserSummaryDto;
import com.markettwin.backend.security.CurrentUserProvider;
import com.markettwin.backend.service.UserAdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 2026-08-05 추가 (회원관리)
 * FE UserAdminPage(관리자 전용 - "사용자 관리"/"회원 승인" 탭)와 대응.
 * 관리자 판정은 UserAdminService에서 함(CurrentUserProvider는 PostController와
 * 동일하게 loginId -> User 조회만 담당).
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserAdminService userAdminService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public List<UserSummaryDto> list(
            @RequestParam(required = false) String marketCode,
            @RequestParam(defaultValue = "false") boolean pendingOnly
    ) {
        User currentUser = currentUserProvider.getCurrentUser();
        return userAdminService.list(marketCode, pendingOnly, currentUser);
    }

    // 2026-08-13 추가: 관리자 당직자 지정/해제 API
    @PatchMapping("/{userId}/duty")
    public UserSummaryDto updateDuty(
            @PathVariable Long userId,
            @Valid @RequestBody com.markettwin.backend.dto.request.UpdateUserDutyRequestDto request,
            HttpServletRequest httpRequest
    ) {
        User currentUser = currentUserProvider.getCurrentUser();
        return userAdminService.updateDuty(userId, request.getIsDuty(), currentUser, resolveClientIp(httpRequest));
    }


    @PatchMapping("/{userId}/role")
    public UserSummaryDto updateRole(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRoleRequestDto request,
            HttpServletRequest httpRequest
    ) {
        User currentUser = currentUserProvider.getCurrentUser();
        return userAdminService.updateRole(userId, request.getRulesCode(), request.getMarketCode(),
                currentUser, resolveClientIp(httpRequest));
    }

    // AuthController와 동일한 패턴(로드밸런서 뒤에서 X-Forwarded-For 우선 사용)
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
