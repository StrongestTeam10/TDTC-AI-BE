package com.markettwin.backend.service;

import com.markettwin.backend.domain.entity.Facility;
import com.markettwin.backend.domain.entity.User;
import com.markettwin.backend.dto.request.FacilityCreateRequestDto;
import com.markettwin.backend.dto.request.FacilityUpdateRequestDto;
import com.markettwin.backend.dto.response.FacilityDto;
import com.markettwin.backend.exception.FacilityNotFoundException;
import com.markettwin.backend.exception.ForbiddenActionException;
import com.markettwin.backend.repository.FacilityPhotoRepository;
import com.markettwin.backend.repository.FacilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * 2026-08-04 추가 (시설 관리 화면 - 상점 위치 등록)
 *
 * 권한 규칙(재재님 확정): 관리자(ROL01) 또는 상인회(usrusrs01m.org_code = 'ORGMA')만
 * 시설을 등록/수정/삭제할 수 있고, 조회(목록)도 이 두 그룹만 가능하다 - FE에서
 * 이 두 그룹이 아니면 탭 자체를 숨기는 것과 동일한 기준을 BE에서도 적용함(탭을
 * 숨기는 건 UX일 뿐이고, 실제 접근 차단은 서버가 매번 재검증해야 함).
 * 시장 범위 검증은 MarketService.getAccessibleMarket을 그대로 재사용(게시판/대시보드와
 * 동일한 "관리자는 전체, 그 외는 본인 담당 시장만" 규칙).
 */
@Service
@RequiredArgsConstructor
public class FacilityService {

    private static final String ADMIN_ROLE_CODE = "ROL01";
    // comcode01m ORG 도메인 - 시장상인회
    private static final String MERCHANT_ASSOCIATION_ORG_CODE = "ORGMA";

    private final FacilityRepository facilityRepository;
    private final FacilityPhotoRepository facilityPhotoRepository;
    private final MarketService marketService;

    public List<FacilityDto> list(Long marketId, User currentUser) {
        assertCanManageFacilities(currentUser);
        marketService.getAccessibleMarket(marketId, currentUser);

        return facilityRepository.findByMarketId(marketId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public FacilityDto create(FacilityCreateRequestDto request, User currentUser) {
        assertCanManageFacilities(currentUser);
        marketService.getAccessibleMarket(request.getMarketId(), currentUser);

        Facility facility = Facility.builder()
                .marketId(request.getMarketId())
                .facilityType(request.getFacilityType())
                .name(request.getName())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .isActive(request.getIsActive() == null || request.getIsActive())
                .rmk(request.getRmk())
                .updatedAt(Instant.now())
                .build();

        return toDto(facilityRepository.save(facility));
    }

    @Transactional
    public FacilityDto update(Long facilityId, FacilityUpdateRequestDto request, User currentUser) {
        assertCanManageFacilities(currentUser);
        Facility facility = getFacilityOrThrow(facilityId);
        marketService.getAccessibleMarket(facility.getMarketId(), currentUser);

        facility.updateDetails(
                request.getFacilityType(), request.getName(),
                request.getLatitude(), request.getLongitude(),
                request.getIsActive(), request.getRmk());

        return toDto(facility);
    }

    @Transactional
    public void delete(Long facilityId, User currentUser) {
        assertCanManageFacilities(currentUser);
        Facility facility = getFacilityOrThrow(facilityId);
        marketService.getAccessibleMarket(facility.getMarketId(), currentUser);

        // mrkfcph01d(시설 사진)는 FK ON DELETE CASCADE라 별도 처리 없이 함께 삭제됨.
        // S3에 올라간 실제 파일까지 지우진 않음(고아 오브젝트 - 이번 범위 밖, 필요하면
        // 추후 별도 정리 배치로 처리).
        facilityRepository.delete(facility);
    }

    private Facility getFacilityOrThrow(Long facilityId) {
        return facilityRepository.findById(facilityId)
                .orElseThrow(() -> new FacilityNotFoundException(facilityId));
    }

    private void assertCanManageFacilities(User user) {
        boolean allowed = ADMIN_ROLE_CODE.equals(user.getRulesCode())
                || MERCHANT_ASSOCIATION_ORG_CODE.equals(user.getOrgCode());
        if (!allowed) {
            throw new ForbiddenActionException("시설 관리 권한이 없습니다.");
        }
    }

    private FacilityDto toDto(Facility facility) {
        long photoCount = facilityPhotoRepository.countByFacilityId(facility.getFacilityId());

        return FacilityDto.builder()
                .facilityId(facility.getFacilityId())
                .marketId(facility.getMarketId())
                .facilityType(facility.getFacilityType())
                .name(facility.getName())
                .latitude(facility.getLatitude())
                .longitude(facility.getLongitude())
                .isActive(facility.getIsActive())
                .rmk(facility.getRmk())
                .photoCount(photoCount)
                .updatedAt(facility.getUpdatedAt())
                .build();
    }
}
