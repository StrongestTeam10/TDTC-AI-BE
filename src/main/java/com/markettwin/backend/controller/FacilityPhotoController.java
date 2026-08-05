package com.markettwin.backend.controller;

import com.markettwin.backend.domain.entity.User;
import com.markettwin.backend.dto.response.FacilityPhotoDto;
import com.markettwin.backend.dto.response.PhotoExifPreviewDto;
import com.markettwin.backend.security.CurrentUserProvider;
import com.markettwin.backend.service.FacilityPhotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

/**
 * 2026-08-04 추가 (상점 외관 직접 촬영 데이터 수집 파이프라인)
 *
 * FE 플로우: (1) POST .../exif로 사진을 보내 EXIF GPS/촬영일시 미리보기를 받아
 * 지도 보정 UI 초기값으로 사용 -> (2) 사용자가 위치 보정 + 방향 라벨링 후
 * POST(저장 엔드포인트)로 같은 파일 + 보정값을 함께 전송해 실제 저장.
 */
@RestController
@RequestMapping("/api/facilities/{facilityId}/photos")
@RequiredArgsConstructor
public class FacilityPhotoController {

    private final FacilityPhotoService facilityPhotoService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping(value = "/exif", consumes = "multipart/form-data")
    public PhotoExifPreviewDto previewExif(
            @PathVariable Long facilityId,
            @RequestParam MultipartFile file
    ) {
        User currentUser = currentUserProvider.getCurrentUser();
        return facilityPhotoService.previewExif(facilityId, file, currentUser);
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<FacilityPhotoDto> save(
            @PathVariable Long facilityId,
            @RequestParam MultipartFile file,
            @RequestParam String directionCode,
            @RequestParam BigDecimal correctedLatitude,
            @RequestParam BigDecimal correctedLongitude
    ) {
        User currentUser = currentUserProvider.getCurrentUser();
        FacilityPhotoDto saved = facilityPhotoService.save(
                facilityId, file, directionCode, correctedLatitude, correctedLongitude, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public List<FacilityPhotoDto> list(@PathVariable Long facilityId) {
        User currentUser = currentUserProvider.getCurrentUser();
        return facilityPhotoService.list(facilityId, currentUser);
    }

    @DeleteMapping("/{photoId}")
    public ResponseEntity<Void> delete(@PathVariable Long facilityId, @PathVariable Long photoId) {
        User currentUser = currentUserProvider.getCurrentUser();
        facilityPhotoService.delete(facilityId, photoId, currentUser);
        return ResponseEntity.noContent().build();
    }
}
