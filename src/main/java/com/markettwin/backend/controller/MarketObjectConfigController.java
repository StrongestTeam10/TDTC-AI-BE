package com.markettwin.backend.controller;

import com.markettwin.backend.dto.request.MarketObjectConfigSaveRequestDto;
import com.markettwin.backend.dto.response.MarketObjectConfigDto;
import com.markettwin.backend.security.CurrentUserProvider;
import com.markettwin.backend.service.MarketObjectConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 2026-08-11 추가 (시장 오브젝트/구조 설정 - 시장 구조 등록 화면).
 *
 * 시뮬레이션 비교의 초기 배치로 쓸 오브젝트 배치 + 통로 제어 정책을 시장당 1세트로
 * 저장/조회한다. 실제 시뮬레이션 실행 결과는 기존 baseline/scenario 흐름 그대로.
 * 권한은 시설 등록과 동일하게 관리자(ROL01) 또는 상인회(ORGMA)만.
 */
@RestController
@RequestMapping("/api/market-objects")
@RequiredArgsConstructor
public class MarketObjectConfigController {

    private final MarketObjectConfigService service;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public MarketObjectConfigDto get(@RequestParam Long marketId) {
        return service.get(marketId, currentUserProvider.getCurrentUser());
    }

    /** 시장당 1세트를 통째로 덮어쓴다(upsert). */
    @PutMapping
    public MarketObjectConfigDto save(@Valid @RequestBody MarketObjectConfigSaveRequestDto request) {
        return service.save(request, currentUserProvider.getCurrentUser());
    }
}
