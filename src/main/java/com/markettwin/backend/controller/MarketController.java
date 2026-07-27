package com.markettwin.backend.controller;

import com.markettwin.backend.domain.entity.User;
import com.markettwin.backend.dto.response.MarketDto;
import com.markettwin.backend.dto.response.ZoneDto;
import com.markettwin.backend.security.CurrentUserProvider;
import com.markettwin.backend.service.MarketService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 2026-07-27 변경: 시장/구역별 권한 분리(관리자는 전체, 그 외는 본인 담당 시장만)
 * 적용을 위해 CurrentUserProvider로 로그인 사용자를 조회해 MarketService에 전달함.
 */
@RestController
@RequestMapping("/api/markets")
@RequiredArgsConstructor
public class MarketController {

    private final MarketService marketService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public List<MarketDto> getMarkets() {
        User currentUser = currentUserProvider.getCurrentUser();
        return marketService.getMarkets(currentUser);
    }

    @GetMapping("/{marketId}/zones")
    public List<ZoneDto> getZones(@PathVariable Long marketId) {
        User currentUser = currentUserProvider.getCurrentUser();
        return marketService.getZones(marketId, currentUser);
    }
}
