package com.markettwin.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.markettwin.backend.domain.entity.User;
import com.markettwin.backend.domain.entity.Zone;
import com.markettwin.backend.dto.request.ZoneSaveRequestDto;
import com.markettwin.backend.dto.request.ZoneSplitRequestDto;
import com.markettwin.backend.dto.response.ZoneDto;
import com.markettwin.backend.exception.InvalidZonePolygonException;
import com.markettwin.backend.exception.ZoneInUseException;
import com.markettwin.backend.exception.ZoneNotFoundException;
import com.markettwin.backend.repository.CctvZoneRepository;
import com.markettwin.backend.repository.ZoneAdjacencyRepository;
import com.markettwin.backend.repository.ZoneRepository;
import com.markettwin.backend.util.GeoJsonPolygons;
import com.markettwin.backend.util.PolygonSplitter;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

/**
 * (시뮬레이션 구역 등록/수정/삭제).
 *
 * 지금까지 구역은 seed-market-data.sql로만 들어갔다. ZoneRepository는 있었지만
 * MarketService·ReportService·SpatialLayoutService·CctvZoneService 네 곳에서
 * 조회로만 쓰였고 저장하는 코드가 한 줄도 없었다. 그래서 새 시장을 추가해도
 * 구역이 없어 CCTV 등록(mrkcctv01m.zone_id가 필수)까지 연쇄로 막혔다.
 *
 * 조회(getZones)는 기존대로 MarketService에 두고, 여기서는 쓰기만 다룬다.
 * 권한은 MarketService.assertCanManageMarkets로 통일한다(관리자 ROL01만).
 */
@Service
@RequiredArgsConstructor
public class ZoneService {

    /** 폴리곤이 되려면 서로 다른 꼭짓점이 최소 3개는 있어야 한다. */
    private static final int MIN_VERTEX_COUNT = 3;

    /** mrkaddr01d.zone_name의 컬럼 길이. 분할 API는 DTO 검증을 못 타서 여기서 확인한다. */
    private static final int MAX_ZONE_NAME_LENGTH = 50;

    private final ZoneRepository zoneRepository;
    private final ZoneAdjacencyRepository zoneAdjacencyRepository;
    private final CctvZoneRepository cctvZoneRepository;
    private final MarketService marketService;
    private final ObjectMapper objectMapper;

    @Transactional
    public ZoneDto createZone(Long marketId, ZoneSaveRequestDto request, User currentUser) {
        marketService.assertCanManageMarkets(currentUser);
        marketService.getAccessibleMarket(marketId, currentUser);

        String zoneName = request.getZoneName().trim();
        assertValidPolygon(request.getPolygonCoordinates());
        assertZoneNameAvailable(marketId, zoneName, null);

        Zone saved = zoneRepository.save(Zone.builder()
                .marketId(marketId)
                .zoneName(zoneName)
                .polygonCoordinates(request.getPolygonCoordinates())
                .build());

        return toDto(saved);
    }

    /**
     * 시장 영역 폴리곤 하나를 선으로 잘라 여러 구역을 한 번에 만든다.
     *
     * 구역을 하나씩 그리는 것(createZone)보다 실제 시장 구조에 가깝다. 망원시장도
     * 골목 하나를 출입구 위치에서 두 번 잘라 3구역이 됐고, 그렇게 만들면 사람이
     * 찍는 점이 40개에서 "영역 한 번 + 선 두 개"로 줄어든다.
     *
     * 이미 구역이 있는 시장에도 쓸 수 있지만, 기존 구역은 건드리지 않고 새 구역만
     * 추가한다(이름이 겹치면 거부).
     */
    @Transactional
    public List<ZoneDto> splitIntoZones(Long marketId, ZoneSplitRequestDto request, User currentUser) {
        marketService.assertCanManageMarkets(currentUser);
        marketService.getAccessibleMarket(marketId, currentUser);

        assertValidPolygon(request.getPolygonCoordinates());

        List<double[]> outerRing;
        List<double[][]> cutLines = new ArrayList<>();
        try {
            outerRing = GeoJsonPolygons.parseOuterRing(request.getPolygonCoordinates(), objectMapper);
            for (String cutLine : request.getCutLines()) {
                cutLines.add(GeoJsonPolygons.parseLineEndpoints(cutLine, objectMapper));
            }
        } catch (IllegalArgumentException e) {
            throw new InvalidZonePolygonException(e.getMessage());
        }

        List<PolygonSplitter.Piece> pieces;
        try {
            pieces = PolygonSplitter.split(outerRing, cutLines);
        } catch (IllegalArgumentException e) {
            throw new InvalidZonePolygonException(e.getMessage());
        }

        // 이름을 순서대로 붙일 수 있게 북 -> 남으로 정렬한다(망원시장 북측/중앙/남측과 같은 순서).
        List<PolygonSplitter.Piece> ordered = pieces.stream()
                .sorted(Comparator.comparingDouble(
                        (PolygonSplitter.Piece piece) -> GeoJsonPolygons.centroid(piece.ring())[1]).reversed())
                .toList();

        List<String> zoneNames = resolveZoneNames(request.getZoneNames(), ordered.size());
        for (String zoneName : zoneNames) {
            assertZoneNameAvailable(marketId, zoneName, null);
        }

        List<ZoneDto> created = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            Zone saved = zoneRepository.save(Zone.builder()
                    .marketId(marketId)
                    .zoneName(zoneNames.get(i))
                    .polygonCoordinates(GeoJsonPolygons.toPolygonJson(ordered.get(i).ring()))
                    .build());
            created.add(toDto(saved));
        }
        return created;
    }

    /** 요청에 이름이 없으면 자동으로 붙이고, 있으면 개수/공백/중복을 확인한다. */
    private List<String> resolveZoneNames(List<String> requested, int pieceCount) {
        if (requested == null || requested.isEmpty()) {
            return IntStream.rangeClosed(1, pieceCount)
                    .mapToObj(index -> "구역 " + index)
                    .toList();
        }
        if (requested.size() != pieceCount) {
            throw new InvalidZonePolygonException(
                    "구역이 " + pieceCount + "개로 나뉘었는데 이름은 " + requested.size() + "개입니다.");
        }

        List<String> names = requested.stream().map(String::trim).toList();
        if (names.stream().anyMatch(String::isEmpty)) {
            throw new InvalidZonePolygonException("빈 구역 이름은 쓸 수 없습니다.");
        }
        if (names.stream().anyMatch(name -> name.length() > MAX_ZONE_NAME_LENGTH)) {
            throw new InvalidZonePolygonException("구역 이름은 50자를 넘을 수 없습니다.");
        }
        // DB 중복(assertZoneNameAvailable)과 별개로, 이번 요청 안에서 겹치는 것도 막는다.
        long distinctCount = names.stream().map(name -> name.toLowerCase(Locale.ROOT)).distinct().count();
        if (distinctCount != names.size()) {
            throw new ZoneInUseException("구역 이름이 서로 겹칩니다.");
        }
        return names;
    }

    /**
     * ⚠️ 폴리곤을 고치면 과거 시뮬레이션 결과(simrslt01d.max_density_zone_id)가
     * 가리키는 구역의 모양이 바뀐다. zone_id는 그대로라 이력은 끊기지 않지만,
     * "그때의 그 구역"과 지금 구역이 같은 범위가 아니게 된다. 관리자(ROL01)만
     * 수정할 수 있게 막아둔 이유다.
     */
    @Transactional
    public ZoneDto updateZone(Long zoneId, ZoneSaveRequestDto request, User currentUser) {
        marketService.assertCanManageMarkets(currentUser);

        Zone zone = getZoneOrThrow(zoneId);
        marketService.getAccessibleMarket(zone.getMarketId(), currentUser);

        String zoneName = request.getZoneName().trim();
        assertValidPolygon(request.getPolygonCoordinates());
        assertZoneNameAvailable(zone.getMarketId(), zoneName, zoneId);

        // Zone은 setter가 없는 불변 엔티티라, 같은 zone_id로 다시 만들어 저장(merge)한다.
        Zone updated = Zone.builder()
                .zoneId(zone.getZoneId())
                .marketId(zone.getMarketId())
                .zoneName(zoneName)
                .polygonCoordinates(request.getPolygonCoordinates())
                .build();

        return toDto(zoneRepository.save(updated));
    }

    @Transactional
    public void deleteZone(Long zoneId, User currentUser) {
        marketService.assertCanManageMarkets(currentUser);

        Zone zone = getZoneOrThrow(zoneId);
        marketService.getAccessibleMarket(zone.getMarketId(), currentUser);

        // 가장 흔한 실패 원인이라 먼저 확인해 구체적인 안내를 준다.
        long cctvCount = cctvZoneRepository.countByZoneId(zoneId);
        if (cctvCount > 0) {
            throw new ZoneInUseException(
                    "이 구역에 등록된 CCTV 구역이 " + cctvCount + "개 있습니다. CCTV를 먼저 옮기거나 삭제해 주세요.");
        }

        // 통로는 구역에서 파생된 값이라 함께 지운다(구역이 없어지면 의미가 없다).
        zoneAdjacencyRepository.deleteByFromZoneIdOrToZoneId(zoneId, zoneId);

        try {
            zoneRepository.delete(zone);
            // flush를 하지 않으면 FK 위반이 트랜잭션 커밋 시점에 터져서
            // 여기서 잡지 못하고 500으로 새어 나간다.
            zoneRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ZoneInUseException(
                    "이 구역을 참조하는 데이터가 있어 삭제할 수 없습니다(시뮬레이션 결과·영상 클립·비상 알림 등).");
        }
    }

    private Zone getZoneOrThrow(Long zoneId) {
        return zoneRepository.findById(zoneId)
                .orElseThrow(() -> new ZoneNotFoundException(zoneId));
    }

    /**
     * DB에 유니크 제약은 없지만, CCTV 등록 화면이 구역을 이름으로 보여주기 때문에
     * 같은 시장 안에 같은 이름이 둘이면 어느 쪽을 고른 건지 알 수 없게 된다.
     */
    private void assertZoneNameAvailable(Long marketId, String zoneName, Long excludeZoneId) {
        boolean taken = zoneRepository.findByMarketId(marketId).stream()
                .filter(z -> !z.getZoneId().equals(excludeZoneId))
                .anyMatch(z -> zoneName.equalsIgnoreCase(z.getZoneName().trim()));
        if (taken) {
            throw new ZoneInUseException("같은 시장에 이미 있는 구역 이름입니다: " + zoneName);
        }
    }

    private void assertValidPolygon(String polygonCoordinates) {
        List<double[]> ring;
        try {
            ring = GeoJsonPolygons.parseOuterRing(polygonCoordinates, objectMapper);
        } catch (IllegalArgumentException e) {
            throw new InvalidZonePolygonException(e.getMessage());
        }

        long distinctCount = GeoJsonPolygons.withoutClosingPoint(ring).stream()
                .map(p -> p[0] + "," + p[1])
                .distinct()
                .count();
        if (distinctCount < MIN_VERTEX_COUNT) {
            throw new InvalidZonePolygonException(
                    "구역은 서로 다른 꼭짓점이 3개 이상이어야 합니다(현재 " + distinctCount + "개).");
        }

        for (double[] point : ring) {
            // 위도 범위(-90~90)를 넘는 값이 나오면 대개 [위도, 경도]로 뒤집어 보낸 것이다.
            // 뒤집힌 좌표는 예외 없이 저장돼 지도에서 엉뚱한 곳을 가리키므로 여기서 막는다.
            if (point[0] < -180 || point[0] > 180 || point[1] < -90 || point[1] > 90) {
                throw new InvalidZonePolygonException(
                        "좌표 범위를 벗어났습니다: [" + point[0] + ", " + point[1] + "]. GeoJSON은 [경도, 위도] 순서입니다.");
            }
        }
    }

    private ZoneDto toDto(Zone zone) {
        return new ZoneDto(
                zone.getZoneId(),
                zone.getMarketId(),
                zone.getZoneName(),
                zone.getPolygonCoordinates()
        );
    }
}
