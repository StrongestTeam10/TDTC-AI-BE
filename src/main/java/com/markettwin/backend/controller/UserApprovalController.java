package com.markettwin.backend.controller;

import com.markettwin.backend.domain.entity.User;
import com.markettwin.backend.dto.response.PendingUserDto;
import com.markettwin.backend.security.CurrentUserProvider;
import com.markettwin.backend.service.UserApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 2026-08-04 추가 (회원가입 관리자 승인)
 * 관리자(ROL01)만 호출 가능 - UserApprovalService.assertAdmin 참고.
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserApprovalController {

    private final UserApprovalService userApprovalService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/pending")
    public List<PendingUserDto> listPending() {
        return userApprovalService.listPending(currentUserProvider.getCurrentUser());
    }

    @PostMapping("/{userId}/approve")
    public ResponseEntity<Void> approve(@PathVariable Long userId) {
        userApprovalService.approve(userId, currentUserProvider.getCurrentUser());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/reject")
    public ResponseEntity<Void> reject(@PathVariable Long userId) {
        userApprovalService.reject(userId, currentUserProvider.getCurrentUser());
        return ResponseEntity.noContent().build();
    }
}
