package com.markettwin.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.markettwin.backend.domain.entity.CctvZone;
import com.markettwin.backend.domain.entity.User;
import com.markettwin.backend.domain.entity.Zone;
import com.markettwin.backend.dto.request.CctvZoneSaveRequestDto;
import com.markettwin.backend.dto.response.CctvZoneDto;
import com.markettwin.backend.dto.response.PageResponseDto;
import com.markettwin.backend.exception.CctvZoneNotFoundException;
import com.markettwin.backend.exception.ForbiddenActionException;
import com.markettwin.backend.exception.InvalidCctvZonePolygonException;
import com.markettwin.backend.repository.CctvZoneRepository;
import com.markettwin.backend.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 2026-08-11 추가 (CCTV 관제 구역). 08-11 2차 재설계.
 *
 * ⚠️ 이 서비스는 mrkcctv01m만 다루고 시뮬레이션 구역(mrkaddr01d)은 읽기만 한다
 * (소속 구역명 조인 + 폴리곤 포함 검증). 두 테이블을 분리한 이유는 CctvZone 주석 참고.
 *
 * 권한 규칙은 FacilityService와 동일 - 관리자(ROL01) 또는 상인회(ORGMA)만.
 */
@Service
@RequiredArgsConstructor
public class CctvZoneService {

    private static final String ADMIN_ROLE_CODE = "ROL01";
    // comcode01m ORG 도메인 - 시장상인회
    private static final String MERCHANT_ASSOCIATION_ORG_CODE = "ORGMA";

    /** CCTV 호모그래피 ROI라 꼭짓점은 사각형 4개로 고정한다. */
    private static final int REQUIRED_VERTEX_COUNT = 4;

    private static final int MAX_PAGE_SIZE = 100;

    private final CctvZoneRepository cctvZoneRepository;
    private final ZoneRepository zoneRepository;
    private final MarketService marketService;
    private final ObjectMapper objectMapper;

    public PageResponseDto<CctvZoneDto> list(Long marketId, int page, int size, User currentUser) {
        assertCanManageCctvZones(currentUser);
        marketService.getAccessibleMarket(marketId, currentUser);

        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "cctvZoneId"));

        Page<CctvZone> resultPage = cctvZoneRepository.findByMarketId(marketId, pageable);

        // 이 페이지에 나오는 zone_id들의 이름을 한 번에 조회해 조인한다(N+1 방지).
        Map<Long, String> zoneNames = zoneRepository.findByMarketId(marketId).stream()
                .collect(Collectors.toMap(Zone::getZoneId, Zone::getZoneName, (a, b) -> a));

        return PageResponseDto.from(
                resultPage.map(z -> CctvZoneDto.from(z, zoneNames.get(z.getZoneId()))));
    }

    /** 등록마다 새 행을 추가한다. */
    @Transactional
    public CctvZoneDto create(CctvZoneSaveRequestDto request, User currentUser) {
        assertCanManageCctvZones(currentUser);
        marketService.getAccessibleMarket(request.getMarketId(), currentUser);

        Zone zone = getZoneInMarketOrThrow(request.getZoneId(), request.getMarketId());
        assertRectangleInsideZone(request.getPolygonCoordinates(), zone);

        CctvZone entity = CctvZone.builder()
                .marketId(request.getMarketId())
                .zoneId(request.getZoneId())
                .polygonCoordinates(request.getPolygonCoordinates())
                .isActive(request.getIsActive() == null || request.getIsActive())
                .rmk(request.getRmk())
                .updatedAt(Instant.now())
                .build();

        return CctvZoneDto.from(cctvZoneRepository.save(entity), zone.getZoneName());
    }

    @Transactional
    public CctvZoneDto update(Long cctvZoneId, CctvZoneSaveRequestDto request, User currentUser) {
        assertCanManageCctvZones(currentUser);
        CctvZone entity = cctvZoneRepository.findById(cctvZoneId)
                .orElseThrow(() -> new CctvZoneNotFoundException(cctvZoneId));
        marketService.getAccessibleMarket(entity.getMarketId(), currentUser);

        Zone zone = getZoneInMarketOrThrow(request.getZoneId(), entity.getMarketId());
        assertRectangleInsideZone(request.getPolygonCoordinates(), zone);

        entity.updateDetails(
                request.getZoneId(),
                request.getPolygonCoordinates(),
                request.getIsActive() == null || request.getIsActive(),
                request.getRmk());

        return CctvZoneDto.from(entity, zone.getZoneName());
    }

    @Transactional
    public void delete(Long cctvZoneId, User currentUser) {
        assertCanManageCctvZones(currentUser);
        CctvZone entity = cctvZoneRepository.findById(cctvZoneId)
                .orElseThrow(() -> new CctvZoneNotFoundException(cctvZoneId));
        marketService.getAccessibleMarket(entity.getMarketId(), currentUser);
        cctvZoneRepository.delete(entity);
    }

    private Zone getZoneInMarketOrThrow(Long zoneId, Long marketId) {
        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new InvalidCctvZonePolygonException("소속 구역을 찾을 수 없습니다."));
        // 다른 시장의 구역을 소속으로 지정하는 것을 막는다.
        if (!zone.getMarketId().equals(marketId)) {
            throw new InvalidCctvZonePolygonException("소속 구역이 이 시장에 속하지 않습니다.");
        }
        return zone;
    }

    private void assertCanManageCctvZones(User currentUser) {
        if (currentUser == null) {
            throw new ForbiddenActionException("로그인이 필요합니다.");
        }
        boolean isAdmin = ADMIN_ROLE_CODE.equals(currentUser.getRulesCode());
        boolean isMerchantAssociation = MERCHANT_ASSOCIATION_ORG_CODE.equals(currentUser.getOrgCode());
        if (!isAdmin && !isMerchantAssociation) {
            throw new ForbiddenActionException("CCTV 구역을 관리할 권한이 없습니다.");
        }
    }

    /**
     * 저장 전 검증: (1) 사각형 4점인지, (2) 네 꼭짓점이 소속 시뮬레이션 구역 폴리곤
     * 안에 있는지.
     *
     * 변(선분)이 오목한 구역 밖으로 삐져나오는 경우는 FE에서 실시간으로 막는다. BE는
     * 꼭짓점 포함 여부를 무결성 백스톱으로 확인한다(FE를 우회한 직접 호출 방어).
     */
    private void assertRectangleInsideZone(String polygonCoordinates, Zone zone) {
        List<double[]> vertices = parseRectangleVertices(polygonCoordinates);

        List<double[]> zoneRing = parseZoneRing(zone.getPolygonCoordinates());
        if (zoneRing.size() < 3) {
            throw new InvalidCctvZonePolygonException("소속 구역의 좌표가 올바르지 않습니다.");
        }

        for (double[] v : vertices) {
            // GeoJSON은 [경도(x), 위도(y)] 순.
            if (!isPointInPolygon(v[0], v[1], zoneRing)) {
                throw new InvalidCctvZonePolygonException("꼭짓점이 소속 구역 밖에 있습니다.");
            }
        }
    }

    /** CCTV 폴리곤 문자열을 파싱해 닫힌 링을 제외한 꼭짓점 4개를 돌려준다. */
    private List<double[]> parseRectangleVertices(String polygonCoordinates) {
        JsonNode ring = readFirstRing(polygonCoordinates);

        int size = ring.size();
        boolean closed = size >= 2 && ring.get(0).equals(ring.get(size - 1));
        int vertexCount = closed ? size - 1 : size;
        if (vertexCount != REQUIRED_VERTEX_COUNT) {
            throw new InvalidCctvZonePolygonException("사각형 4점이어야 합니다(현재 " + vertexCount + "점).");
        }

        return java.util.stream.IntStream.range(0, vertexCount)
                .mapToObj(i -> new double[]{ring.get(i).get(0).asDouble(), ring.get(i).get(1).asDouble()})
                .collect(Collectors.toList());
    }

    /** 시뮬레이션 구역 링을 [경도, 위도] 점 목록으로 파싱한다. */
    private List<double[]> parseZoneRing(String polygonCoordinates) {
        JsonNode ring = readFirstRing(polygonCoordinates);
        return java.util.stream.StreamSupport.stream(ring.spliterator(), false)
                .filter(pt -> pt.isArray() && pt.size() >= 2)
                .map(pt -> new double[]{pt.get(0).asDouble(), pt.get(1).asDouble()})
                .collect(Collectors.toList());
    }

    private JsonNode readFirstRing(String polygonCoordinates) {
        JsonNode root;
        try {
            root = objectMapper.readTree(polygonCoordinates);
        } catch (Exception e) {
            throw new InvalidCctvZonePolygonException("JSON 형식이 아닙니다.");
        }
        JsonNode coordinates = root.path("coordinates");
        if (!coordinates.isArray() || coordinates.isEmpty() || !coordinates.get(0).isArray()) {
            throw new InvalidCctvZonePolygonException("GeoJSON Polygon 형식이 아닙니다.");
        }
        return coordinates.get(0);
    }

    /**
     * 점이 폴리곤 안에 있는지 판정(ray casting). 경계선 위 점도 안으로 본다.
     * ring은 [경도(x), 위도(y)] 점 목록.
     */
    private boolean isPointInPolygon(double x, double y, List<double[]> ring) {
        boolean inside = false;
        int n = ring.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = ring.get(i)[0], yi = ring.get(i)[1];
            double xj = ring.get(j)[0], yj = ring.get(j)[1];

            boolean intersect = ((yi > y) != (yj > y))
                    && (x < (xj - xi) * (y - yi) / (yj - yi) + xi);
            if (intersect) inside = !inside;
        }
        return inside;
    }
}
