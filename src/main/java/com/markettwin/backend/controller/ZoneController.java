package com.markettwin.backend.controller;

import com.markettwin.backend.dto.request.ZoneSaveRequestDto;
import com.markettwin.backend.dto.response.ZoneDto;
import com.markettwin.backend.security.CurrentUserProvider;
import com.markettwin.backend.service.ZoneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * (시뮬레이션 구역 수정/삭제).
 *
 * 등록은 시장에 종속되므로 POST /api/markets/{marketId}/zones(MarketController)에
 * 두고, 이미 만들어진 구역을 다루는 수정·삭제만 여기 모았다. 조회는 기존
 * GET /api/markets/{marketId}/zones 그대로다.
 *
 * ⚠️ CCTV 관제 구역(mrkcctv01m, /api/cctv-zones)과는 다른 테이블이다. 이쪽은
 * 시뮬레이션이 에이전트를 움직이는 단위(mrkaddr01d)다.
 *
 * 권한은 관리자(ROL01)만이며 ZoneService에서 검증한다.
 */
@RestController
@RequestMapping("/api/zones")
@RequiredArgsConstructor
public class ZoneController {

    private final ZoneService zoneService;
    private final CurrentUserProvider currentUserProvider;

    @PutMapping("/{zoneId}")
    public ZoneDto update(
            @PathVariable Long zoneId,
            @Valid @RequestBody ZoneSaveRequestDto request) {
        return zoneService.updateZone(zoneId, request, currentUserProvider.getCurrentUser());
    }

    @DeleteMapping("/{zoneId}")
    public ResponseEntity<Void> delete(@PathVariable Long zoneId) {
        zoneService.deleteZone(zoneId, currentUserProvider.getCurrentUser());
        return ResponseEntity.noContent().build();
    }
}
