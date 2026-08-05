package com.markettwin.backend.controller;

import com.markettwin.backend.domain.entity.User;
import com.markettwin.backend.dto.request.FacilityCreateRequestDto;
import com.markettwin.backend.dto.request.FacilityUpdateRequestDto;
import com.markettwin.backend.dto.response.FacilityDto;
import com.markettwin.backend.security.CurrentUserProvider;
import com.markettwin.backend.service.FacilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 2026-08-04 추가 (시설 관리 화면 - 상점 위치 등록)
 * 관리자(ROL01) 또는 상인회(ORGMA)만 호출 가능 - FacilityService.assertCanManageFacilities
 * 참고. 그 외 역할은 403을 받으며, FE도 이 화면 탭 자체를 숨김.
 */
@RestController
@RequestMapping("/api/facilities")
@RequiredArgsConstructor
public class FacilityController {

    private final FacilityService facilityService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public List<FacilityDto> list(@RequestParam Long marketId) {
        return facilityService.list(marketId, currentUserProvider.getCurrentUser());
    }

    @PostMapping
    public ResponseEntity<FacilityDto> create(@Valid @RequestBody FacilityCreateRequestDto request) {
        FacilityDto created = facilityService.create(request, currentUserProvider.getCurrentUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{facilityId}")
    public FacilityDto update(@PathVariable Long facilityId, @Valid @RequestBody FacilityUpdateRequestDto request) {
        return facilityService.update(facilityId, request, currentUserProvider.getCurrentUser());
    }

    @DeleteMapping("/{facilityId}")
    public ResponseEntity<Void> delete(@PathVariable Long facilityId) {
        facilityService.delete(facilityId, currentUserProvider.getCurrentUser());
        return ResponseEntity.noContent().build();
    }
}
