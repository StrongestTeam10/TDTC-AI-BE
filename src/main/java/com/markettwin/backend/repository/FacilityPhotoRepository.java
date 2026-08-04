package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.FacilityPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FacilityPhotoRepository extends JpaRepository<FacilityPhoto, Long> {

    List<FacilityPhoto> findByFacilityIdOrderByCreatedAtDesc(Long facilityId);

    // 2026-08-04 추가 (시설 관리 화면): 목록 테이블에 시설별 사진 개수를 가볍게 표시하기 위함
    long countByFacilityId(Long facilityId);
}
