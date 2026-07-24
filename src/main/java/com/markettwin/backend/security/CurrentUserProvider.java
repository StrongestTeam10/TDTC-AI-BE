package com.markettwin.backend.security;

import com.markettwin.backend.domain.entity.User;
import com.markettwin.backend.exception.InvalidCredentialsException;
import com.markettwin.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 2026-07-24 추가 (게시판 기능)
 *
 * JwtAuthenticationFilter는 인증 성공 시 SecurityContext의 principal(authentication.getName())로
 * loginId만 채워둔다(기존 AuthController#me와 동일 패턴). 게시판 권한 판정(본인 글인지,
 * 담당 시장이 무엇인지, 관리자인지)에는 User 엔티티 전체가 필요해서, 매번 반복되던
 * "loginId로 User 조회" 로직을 공통 컴포넌트로 뽑아둠.
 *
 * DB 조회가 요청마다 1회 추가되는 구조이지만, 이 프로젝트 규모에서는 성능에 영향이
 * 없다고 판단함. 만약 트래픽이 늘어 부담이 되면 JwtTokenProvider가 발급하는 토큰
 * claim에 userId/marketCode를 추가하고 JwtAuthenticationFilter가 커스텀 principal
 * 객체를 SecurityContext에 세팅하도록 바꾸는 방향으로 최적화할 수 있음(현재는 기존
 * 인증 필터 구조를 그대로 유지하는 쪽을 택함).
 */
@Component
@RequiredArgsConstructor
public class CurrentUserProvider {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAnonymous = authentication == null
                || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated();
        if (isAnonymous) {
            throw new InvalidCredentialsException();
        }

        String loginId = authentication.getName();
        return userRepository.findByLoginId(loginId)
                .orElseThrow(InvalidCredentialsException::new);
    }
}
