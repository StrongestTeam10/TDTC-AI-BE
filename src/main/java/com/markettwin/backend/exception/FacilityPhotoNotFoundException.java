package com.markettwin.backend.exception;

// 2026-08-04 추가 (상점 외관 직접 촬영 데이터 수집 파이프라인)
public class FacilityPhotoNotFoundException extends RuntimeException {
    public FacilityPhotoNotFoundException(Long photoId) {
        super("시설 사진을 찾을 수 없습니다: " + photoId);
    }
}
