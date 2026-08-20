package com.markettwin.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.markettwin.backend.client.VworldBuildingClient;
import com.markettwin.backend.domain.entity.Building;
import com.markettwin.backend.domain.entity.Market;
import com.markettwin.backend.domain.entity.User;
import com.markettwin.backend.domain.entity.Zone;
import com.markettwin.backend.dto.request.BuildingBoundsDto;
import com.markettwin.backend.dto.request.BuildingImportRequestDto;
import com.markettwin.backend.dto.request.BuildingPruneRequestDto;
import com.markettwin.backend.dto.response.BuildingImportResultDto;
import com.markettwin.backend.dto.response.BuildingPruneResultDto;
import com.markettwin.backend.exception.BuildingImportConflictException;
import com.markettwin.backend.exception.BuildingImportException;
import com.markettwin.backend.repository.BuildingRepository;
import com.markettwin.backend.repository.ZoneRepository;
import com.markettwin.backend.util.GeoJsonPolygons;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 브이월드에서 받은 건물 폴리곤을 mrkbldg01m에 적재한다.
 *
 * 이 데이터가 필요한 이유는 지도 표시가 아니라 시뮬레이션이다. SIM은 걸을 수 있는
 * 영역을 "구역 폴리곤 합집합 - 건물"로 계산하므로(app/simulation/model.py), 건물이
 * 없으면 에이전트가 상가 자리를 그냥 통과해 밀집도와 대피 시간이 실제와 벌어진다.
 *
 * 권한은 관리자(ROL01)만이며 MarketService.assertCanManageMarkets로 통일한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BuildingImportService {

    /** 시장 하나가 들어오는 정도. 망원시장(길이 약 250m)이 여유 있게 덮인다. */
    private static final int DEFAULT_RADIUS_METERS = 150;

    /**
     * 층당 높이(m). 브이월드가 높이를 주지 않고 지상층수만 줘서 이 값으로 환산한다.
     * 추정값이라는 사실은 height_estimated=true로 남긴다.
     */
    private static final BigDecimal METERS_PER_FLOOR = new BigDecimal("3.0");

    private final BuildingRepository buildingRepository;
    private final ZoneRepository zoneRepository;
    private final VworldBuildingClient vworldBuildingClient;
    private final MarketService marketService;
    private final ObjectMapper objectMapper;

    /** 구역 경계에서 이 거리 안이면 시장과 관련 있는 건물로 본다. */
    private static final int DEFAULT_PRUNE_BUFFER_METERS = 30;

    /** 사각형 범위의 한 변 허용 범위. 위도 1도 = 111.32km 근사를 함께 쓴다. */
    private static final int MIN_BOUNDS_SIDE_METERS = 20;
    private static final int MAX_BOUNDS_SIDE_METERS = 2000;
    private static final double METERS_PER_DEGREE_LAT = 111_320.0;

    /**
     * 구역에서 멀리 떨어진 건물을 지운다.
     *
     * 건물은 시장 중심 기준 <b>반경</b>으로 받아오기 때문에, 구역이 확정되고 나면 시장과
     * 상관없는 건물이 섞여 있다(광장시장 반경 150m 조회에서 실제로 확인). 시뮬레이션은
     * 구역 안만 쓰므로 계산 결과는 그대로지만, 지도가 지저분해지고 저장 공간이 낭비된다.
     *
     * 건물의 중심점과 구역 경계 사이 거리로 판정한다. 폴리곤끼리의 정확한 거리가 아니라
     * 근사지만, 건물 하나가 수십 m 규모라 여유 거리(기본 30m)를 두면 충분하다.
     *
     * dryRun으로 먼저 몇 개가 대상인지 보여주고 사람이 누른 뒤에 지운다 - 되돌릴 수 없는
     * 작업이라 숫자를 보지 않고 실행하게 두지 않는다.
     */
    @Transactional
    public BuildingPruneResultDto pruneDistantBuildings(
            Long marketId, BuildingPruneRequestDto request, User currentUser) {

        marketService.assertCanManageMarkets(currentUser);
        marketService.getAccessibleMarket(marketId, currentUser);

        List<Zone> zones = zoneRepository.findByMarketId(marketId);
        if (zones.isEmpty()) {
            throw new BuildingImportException(
                    "이 시장에는 구역이 없어 기준으로 삼을 경계가 없습니다. 구역을 먼저 나눠 주세요.");
        }

        List<List<double[]>> zoneRings = new ArrayList<>();
        for (Zone zone : zones) {
            if (zone.getPolygonCoordinates() == null) {
                continue;
            }
            try {
                zoneRings.add(GeoJsonPolygons.parseOuterRing(zone.getPolygonCoordinates(), objectMapper));
            } catch (IllegalArgumentException e) {
                log.warn("구역 {}의 좌표를 읽지 못해 판정에서 제외합니다.", zone.getZoneId());
            }
        }
        if (zoneRings.isEmpty()) {
            throw new BuildingImportException("구역 좌표를 읽을 수 없어 정리할 수 없습니다.");
        }

        int bufferMeters = request.getBufferMeters() == null
                ? DEFAULT_PRUNE_BUFFER_METERS
                : request.getBufferMeters();
        boolean dryRun = !Boolean.FALSE.equals(request.getDryRun());

        List<Building> all = buildingRepository.findByMarketId(marketId);
        List<Building> distant = new ArrayList<>();
        for (Building building : all) {
            if (isFarFromEveryZone(building, zoneRings, bufferMeters)) {
                distant.add(building);
            }
        }

        if (!dryRun && !distant.isEmpty()) {
            buildingRepository.deleteAll(distant);
            log.info("시장 {} 건물 정리: {}개 중 {}개 삭제(여유 {}m).",
                    marketId, all.size(), distant.size(), bufferMeters);
        }

        return new BuildingPruneResultDto(
                all.size(), distant.size(), all.size() - distant.size(), bufferMeters, dryRun);
    }

    /**
     * 지정한 사각형이 지나치게 크지 않은지 본다.
     *
     * 브이월드는 한 페이지 1000건이라 넓은 영역을 잡으면 페이지가 급격히 늘고, 시장과
     * 무관한 건물이 수천 개 쌓여 지도와 시뮬레이션 격자만 무거워진다. 한 변 2km면
     * 국내 어느 전통시장이든 충분히 덮는다.
     */
    private void assertBoundsReasonable(double minLon, double minLat, double maxLon, double maxLat) {
        double heightMeters = (maxLat - minLat) * METERS_PER_DEGREE_LAT;
        double widthMeters = (maxLon - minLon) * METERS_PER_DEGREE_LAT
                * Math.cos(Math.toRadians((minLat + maxLat) / 2));

        if (heightMeters < MIN_BOUNDS_SIDE_METERS || widthMeters < MIN_BOUNDS_SIDE_METERS) {
            throw new BuildingImportException("선택한 영역이 너무 좁습니다. 조금 더 넓게 지정해 주세요.");
        }
        if (heightMeters > MAX_BOUNDS_SIDE_METERS || widthMeters > MAX_BOUNDS_SIDE_METERS) {
            throw new BuildingImportException(String.format(Locale.ROOT,
                    "선택한 영역이 너무 넓습니다(%.0fm x %.0fm). 한 변 %dm 이내로 지정해 주세요.",
                    widthMeters, heightMeters, MAX_BOUNDS_SIDE_METERS));
        }
    }

    /** 건물 중심이 모든 구역에서 여유 거리보다 멀면 "시장과 상관없는 건물"로 본다. */
    private boolean isFarFromEveryZone(Building building, List<List<double[]>> zoneRings, int bufferMeters) {
        List<double[]> ring;
        try {
            ring = GeoJsonPolygons.parseOuterRing(building.getPolygonCoordinates(), objectMapper);
        } catch (IllegalArgumentException e) {
            // 좌표가 깨진 건물은 어차피 시뮬레이션에서 못 쓰지만, 지우는 판단까지
            // 자동으로 하지는 않는다(사람이 볼 수 있게 남겨둔다).
            return false;
        }
        double[] center = GeoJsonPolygons.centroid(ring);
        for (List<double[]> zoneRing : zoneRings) {
            if (GeoJsonPolygons.distanceToRingMeters(zoneRing, center[0], center[1]) <= bufferMeters) {
                return false;
            }
        }
        return true;
    }

    @Transactional
    public BuildingImportResultDto importBuildings(
            Long marketId, BuildingImportRequestDto request, User currentUser) {

        marketService.assertCanManageMarkets(currentUser);
        Market market = marketService.getAccessibleMarket(marketId, currentUser);

        if (market.getLatitude() == null || market.getLongitude() == null) {
            throw new BuildingImportException(
                    "시장 중심 좌표가 없어 주변 건물을 조회할 수 없습니다: " + market.getMarketName());
        }

        boolean overwrite = Boolean.TRUE.equals(request.getOverwrite());
        long existingCount = buildingRepository.countByMarketId(marketId);
        if (existingCount > 0 && !overwrite) {
            throw new BuildingImportConflictException(
                    "이 시장에는 이미 건물 " + existingCount + "개가 있습니다. "
                            + "덮어쓰려면 overwrite를 지정해 주세요.");
        }

        // 외부 조회를 먼저 한다. 받아오지 못하면 기존 데이터를 지우지 않고 그대로 끝난다.
        BuildingBoundsDto bounds = request.getBounds();
        boolean usedBounds = bounds != null;
        int radiusMeters = usedBounds
                ? 0
                : (request.getRadiusMeters() == null ? DEFAULT_RADIUS_METERS : request.getRadiusMeters());

        VworldBuildingClient.FetchResult fetched;
        if (usedBounds) {
            // 드래그 방향과 무관하게 남서/북동으로 정렬한다.
            double minLon = Math.min(bounds.getMinLongitude().doubleValue(), bounds.getMaxLongitude().doubleValue());
            double maxLon = Math.max(bounds.getMinLongitude().doubleValue(), bounds.getMaxLongitude().doubleValue());
            double minLat = Math.min(bounds.getMinLatitude().doubleValue(), bounds.getMaxLatitude().doubleValue());
            double maxLat = Math.max(bounds.getMinLatitude().doubleValue(), bounds.getMaxLatitude().doubleValue());
            assertBoundsReasonable(minLon, minLat, maxLon, maxLat);
            fetched = vworldBuildingClient.fetchInBounds(minLon, minLat, maxLon, maxLat);
        } else {
            fetched = vworldBuildingClient.fetchAround(
                    market.getLatitude(), market.getLongitude(), radiusMeters);
        }

        if (fetched.buildings().isEmpty()) {
            throw new BuildingImportException(
                    "반경 " + radiusMeters + "m 안에서 건물을 찾지 못했습니다. "
                            + "시장 중심 좌표와 반경을 확인해 주세요.");
        }

        int deletedCount = 0;
        if (overwrite && existingCount > 0) {
            buildingRepository.deleteByMarketId(marketId);
            deletedCount = (int) existingCount;
            log.warn("시장 {}({})의 기존 건물 {}개를 삭제하고 다시 적재합니다.",
                    marketId, market.getMarketName(), deletedCount);
        }

        // pnu_code에는 전체 UNIQUE 제약이 걸려 있다(mrkbldg01m_pnu_code_key). 중복이 하나라도
        // 섞이면 적재 전체가 실패하므로, 넣기 전에 두 겹으로 걸러낸다.
        //   (1) 응답 안에서의 중복 - 같은 건물이 페이지 경계에 걸쳐 두 번 올 수 있다.
        //   (2) 이미 DB에 있는 것 - 이웃 시장의 반경과 겹치면 같은 건물이 이미 들어와 있다.
        Map<String, VworldBuildingClient.FetchedBuilding> byPnuCode = new LinkedHashMap<>();
        for (VworldBuildingClient.FetchedBuilding fetchedBuilding : fetched.buildings()) {
            byPnuCode.putIfAbsent(fetchedBuilding.pnuCode(), fetchedBuilding);
        }
        Set<String> alreadyStored = new HashSet<>(
                buildingRepository.findExistingPnuCodes(byPnuCode.keySet()));

        Instant now = Instant.now();
        List<Building> entities = new ArrayList<>();
        for (VworldBuildingClient.FetchedBuilding fetchedBuilding : byPnuCode.values()) {
            if (alreadyStored.contains(fetchedBuilding.pnuCode())) {
                continue;
            }
            entities.add(Building.builder()
                    .marketId(marketId)
                    .pnuCode(fetchedBuilding.pnuCode())
                    .polygonCoordinates(fetchedBuilding.polygonCoordinates())
                    .floors(fetchedBuilding.floors())
                    .heightM(METERS_PER_FLOOR.multiply(BigDecimal.valueOf(fetchedBuilding.floors())))
                    .heightEstimated(true)
                    .createdAt(now)
                    .build());
        }
        buildingRepository.saveAll(entities);

        int skippedCount = fetched.buildings().size() - entities.size();
        log.info("시장 {}({}) 건물 적재 완료: 반경 {}m, 브이월드 {}건 -> {}행 저장(중복 {}건 제외, 페이지 {}).",
                marketId, market.getMarketName(), radiusMeters,
                fetched.featureCount(), entities.size(), skippedCount, fetched.pages());

        return new BuildingImportResultDto(
                marketId,
                radiusMeters,
                usedBounds,
                fetched.featureCount(),
                entities.size(),
                skippedCount,
                deletedCount,
                fetched.pages());
    }
}
