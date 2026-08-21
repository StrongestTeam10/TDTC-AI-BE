package com.markettwin.backend.service;

import com.markettwin.backend.domain.entity.Facility;
import com.markettwin.backend.domain.entity.FacilityPhoto;
import com.markettwin.backend.domain.entity.Market;
import com.markettwin.backend.domain.entity.User;
import com.markettwin.backend.dto.response.FacilityPhotoDto;
import com.markettwin.backend.dto.response.PhotoExifPreviewDto;
import com.markettwin.backend.exception.FacilityNotFoundException;
import com.markettwin.backend.exception.FacilityPhotoNotFoundException;
import com.markettwin.backend.exception.ForbiddenActionException;
import com.markettwin.backend.exception.InvalidDirectionCodeException;
import com.markettwin.backend.exception.MarketNotFoundException;
import com.markettwin.backend.repository.FacilityPhotoRepository;
import com.markettwin.backend.repository.FacilityRepository;
import com.markettwin.backend.repository.MarketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.markettwin.backend.util.UploadFiles;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * 상점 외관 직접 촬영 데이터 수집 파이프라인
 *
 * 흐름: previewExif(사진만 보내 EXIF 미리보기, 저장 없음) -> FE 지도 보정 UI ->
 * save(같은 사진 + 보정된 좌표 + 방향 코드를 함께 보내 실제 저장).
 * 두 단계로 나눈 이유: 사용자가 보정을 취소하면 S3/DB에 아무 흔적도 남기지 않기
 * 위함(고아 데이터 방지). 대신 파일을 두 번 전송하게 되는데, 관리자용 저빈도
 * 업로드 도구라 트래픽 부담은 무시할 수준으로 판단.
 */
@Service
@RequiredArgsConstructor
public class FacilityPhotoService {

    private static final String ADMIN_ROLE_CODE = "ROL01";
    // 시설 관리 화면 전체(FacilityService와 동일)를 관리자+상인회
    // 전용으로 좁히면서, 사진 기능도 같은 기준으로 맞춤(기존엔 관리자 또는 "본인 담당
    // 시장이면 역할 무관 접근 가능"이었는데, 이제 상인회가 아닌 일반 조회자/관제요원은
    // 담당 시장이 같아도 접근 불가)
    private static final String MERCHANT_ASSOCIATION_ORG_CODE = "ORGMA";
    private static final Duration DOWNLOAD_URL_TTL = Duration.ofMinutes(5);
    private static final Set<String> VALID_DIRECTION_CODES = Set.of("DIRNO", "DIREA", "DIRSO", "DIRWE");

    private final FacilityRepository facilityRepository;
    private final FacilityPhotoRepository facilityPhotoRepository;
    private final MarketRepository marketRepository;
    private final FileStorageService fileStorageService;
    private final ExifGpsExtractor exifGpsExtractor;

    public PhotoExifPreviewDto previewExif(Long facilityId, MultipartFile file, User currentUser) {
        Facility facility = getFacilityOrThrow(facilityId);
        assertAccessible(facility, currentUser);

        ExifGpsExtractor.Result result = exifGpsExtractor.extract(file);
        return PhotoExifPreviewDto.builder()
                .hasGps(result.hasGps())
                .exifLatitude(result.latitude())
                .exifLongitude(result.longitude())
                .capturedAt(result.capturedAt())
                .build();
    }

    @Transactional
    public FacilityPhotoDto save(Long facilityId, MultipartFile file, String directionCode,
                                  BigDecimal correctedLatitude, BigDecimal correctedLongitude,
                                  User currentUser) {
        Facility facility = getFacilityOrThrow(facilityId);
        assertAccessible(facility, currentUser);

        if (!VALID_DIRECTION_CODES.contains(directionCode)) {
            throw new InvalidDirectionCodeException(directionCode);
        }

        // 추가(보안 감사 BE-09): 상점 외관 "사진"이므로 이미지만 받는다.
        // 아래 EXIF 추출도 이미지를 전제로 동작한다.
        UploadFiles.requireAllowedExtension(
                UploadFiles.sanitizeName(file.getOriginalFilename()),
                UploadFiles.IMAGE_EXTENSIONS, "상점 사진");

        // 저장 시점에도 다시 추출(FE가 미리보기 때 받은 값을 그대로 되돌려 보내는 걸
        // 신뢰하지 않고, 서버가 최종 진실을 갖도록 함 - previewExif와 동일한 파일이라면
        // 결과도 항상 같아 중복 계산 비용 외의 문제는 없음)
        ExifGpsExtractor.Result exif = exifGpsExtractor.extract(file);

        String s3Key = fileStorageService.upload(file, "facility-photos/" + facilityId);

        FacilityPhoto photo = FacilityPhoto.builder()
                .facilityId(facilityId)
                .directionCode(directionCode)
                .s3Key(s3Key)
                .originalName(file.getOriginalFilename())
                .exifLatitude(exif.latitude())
                .exifLongitude(exif.longitude())
                .correctedLatitude(correctedLatitude)
                .correctedLongitude(correctedLongitude)
                .capturedAt(exif.capturedAt())
                .uploadedBy(currentUser.getUserId())
                .createdAt(Instant.now())
                .build();

        FacilityPhoto saved = facilityPhotoRepository.save(photo);
        return toDto(saved);
    }

    public List<FacilityPhotoDto> list(Long facilityId, User currentUser) {
        Facility facility = getFacilityOrThrow(facilityId);
        assertAccessible(facility, currentUser);

        return facilityPhotoRepository.findByFacilityIdOrderByCreatedAtDesc(facilityId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void delete(Long facilityId, Long photoId, User currentUser) {
        Facility facility = getFacilityOrThrow(facilityId);
        assertAccessible(facility, currentUser);

        FacilityPhoto photo = facilityPhotoRepository.findById(photoId)
                .orElseThrow(() -> new FacilityPhotoNotFoundException(photoId));
        if (!photo.getFacilityId().equals(facilityId)) {
            throw new FacilityPhotoNotFoundException(photoId);
        }

        fileStorageService.delete(photo.getS3Key());
        facilityPhotoRepository.delete(photo);
    }

    private FacilityPhotoDto toDto(FacilityPhoto photo) {
        URL downloadUrl = fileStorageService.generatePresignedDownloadUrl(
                photo.getS3Key(), photo.getOriginalName(), DOWNLOAD_URL_TTL);

        return FacilityPhotoDto.builder()
                .photoId(photo.getPhotoId())
                .facilityId(photo.getFacilityId())
                .directionCode(photo.getDirectionCode())
                .originalName(photo.getOriginalName())
                .downloadUrl(downloadUrl)
                .exifLatitude(photo.getExifLatitude())
                .exifLongitude(photo.getExifLongitude())
                .correctedLatitude(photo.getCorrectedLatitude())
                .correctedLongitude(photo.getCorrectedLongitude())
                .capturedAt(photo.getCapturedAt())
                .createdAt(photo.getCreatedAt())
                .build();
    }

    private Facility getFacilityOrThrow(Long facilityId) {
        return facilityRepository.findById(facilityId)
                .orElseThrow(() -> new FacilityNotFoundException(facilityId));
    }

    // 관리자(ROL01) 또는 상인회(ORGMA, 본인 담당 시장에 한해)만 접근
    // 가능 - FacilityService.assertCanManageFacilities와 동일한 기준
    private void assertAccessible(Facility facility, User currentUser) {
        if (isAdmin(currentUser)) {
            return;
        }
        if (!MERCHANT_ASSOCIATION_ORG_CODE.equals(currentUser.getOrgCode())) {
            throw new ForbiddenActionException("시설 관리 권한이 없습니다.");
        }
        Market market = marketRepository.findById(facility.getMarketId())
                .orElseThrow(() -> new MarketNotFoundException(facility.getMarketId()));
        boolean sameMarket = market.getMarketCode() != null
                && market.getMarketCode().equals(currentUser.getMarketCode());
        if (!sameMarket) {
            throw new ForbiddenActionException("본인 담당 시장의 시설이 아닙니다.");
        }
    }

    private boolean isAdmin(User user) {
        return ADMIN_ROLE_CODE.equals(user.getRulesCode());
    }
}
