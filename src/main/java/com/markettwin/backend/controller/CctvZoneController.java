package com.markettwin.backend.controller;

import com.markettwin.backend.dto.request.CctvZoneSaveRequestDto;
import com.markettwin.backend.dto.response.CctvZoneDto;
import com.markettwin.backend.dto.response.PageResponseDto;
import com.markettwin.backend.security.CurrentUserProvider;
import com.markettwin.backend.service.CctvZoneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 2026-08-11 추가 (CCTV 관제 구역 - 상점 위치 등록 화면). 08-11 2차 재설계.
 *
 * ⚠️ 시뮬레이션 구역 조회(GET /api/markets/{marketId}/zones)와는 다른 테이블(mrkcctv01m)을
 * 다룬다. 여기서 구역을 등록해도 시뮬레이션 비교 화면이나 SIM 계산에는 영향이 없다.
 *
 * 등록마다 행이 늘어나는 구조라 저장은 POST(신규)/PUT(수정, id 지정)로 나뉘고,
 * 목록은 게시판과 같은 방식으로 페이징한다. 권한은 관리자(ROL01) 또는 상인회(ORGMA)만.
 */
@RestController
@RequestMapping("/api/cctv-zones")
@RequiredArgsConstructor
public class CctvZoneController {

    private final CctvZoneService cctvZoneService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public PageResponseDto<CctvZoneDto> list(
            @RequestParam Long marketId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {
        return cctvZoneService.list(marketId, page, size, currentUserProvider.getCurrentUser());
    }

    @PostMapping
    public ResponseEntity<CctvZoneDto> create(@Valid @RequestBody CctvZoneSaveRequestDto request) {
        CctvZoneDto created = cctvZoneService.create(request, currentUserProvider.getCurrentUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{cctvZoneId}")
    public CctvZoneDto update(
            @PathVariable Long cctvZoneId,
            @Valid @RequestBody CctvZoneSaveRequestDto request) {
        return cctvZoneService.update(cctvZoneId, request, currentUserProvider.getCurrentUser());
    }

    @DeleteMapping("/{cctvZoneId}")
    public ResponseEntity<Void> delete(@PathVariable Long cctvZoneId) {
        cctvZoneService.delete(cctvZoneId, currentUserProvider.getCurrentUser());
        return ResponseEntity.noContent().build();
    }
}
