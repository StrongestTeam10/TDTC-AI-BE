package com.markettwin.backend.exception;

// 상점 외관 직접 촬영 데이터 수집 파이프라인
public class FacilityNotFoundException extends RuntimeException {
    public FacilityNotFoundException(Long facilityId) {
        super("시설을 찾을 수 없습니다: " + facilityId);
    }
}
