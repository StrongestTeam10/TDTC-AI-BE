package com.markettwin.backend.service;

import com.markettwin.backend.domain.entity.Building;
import com.markettwin.backend.domain.entity.Facility;
import com.markettwin.backend.domain.entity.User;
import com.markettwin.backend.domain.entity.Market;
import com.markettwin.backend.domain.entity.Zone;
import com.markettwin.backend.domain.entity.ZoneAdjacency;
import com.markettwin.backend.dto.response.BuildingDto;
import com.markettwin.backend.dto.response.GateDto;
import com.markettwin.backend.dto.response.MarketDto;
import com.markettwin.backend.dto.response.ZoneAdjacencyDto;
import com.markettwin.backend.dto.response.ZoneDto;
import com.markettwin.backend.repository.BuildingRepository;
import com.markettwin.backend.repository.FacilityRepository;
import com.markettwin.backend.exception.ForbiddenActionException;
import com.markettwin.backend.exception.MarketNotFoundException;
import com.markettwin.backend.repository.MarketRepository;
import com.markettwin.backend.repository.ZoneAdjacencyRepository;
import com.markettwin.backend.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 2026-07-27 추가
 *
 * 권한 규칙(PostService의 게시판 시장 권한과 동일한 패턴):
 *  - 시장/구역 조회: 관리자(ROL01)는 전체 시장을 볼 수 있고, 대시보드에서 시장을
 *    전환할 수 있음. 그 외(상인회 ORGMA/지자체 ORGGV 등 일반 사용자)는 본인
 *    담당 시장(usrusrs01m.market_code)에 해당하는 시장/구역만 조회 가능.
 *  - 클라이언트가 보낸 marketId를 그대로 신뢰하지 않고, 요청된 marketId가 실제로
 *    본인 담당 시장인지 서버가 매번 재검증한다(getAccessibleMarket).
 */
@Service
@RequiredArgsConstructor
public class MarketService {

    private static final String ADMIN_ROLE_CODE = "ROL01";

    private final MarketRepository marketRepository;
    private final ZoneRepository zoneRepository;
    private final ZoneAdjacencyRepository zoneAdjacencyRepository;
    private final FacilityRepository facilityRepository;
    private final BuildingRepository buildingRepository;

    public List<MarketDto> getMarkets(User currentUser) {
        List<Market> markets = isAdmin(currentUser)
                ? marketRepository.findAll()
                : marketRepository.findByMarketCode(currentUser.getMarketCode());

        return markets.stream()
                .map(this::toMarketDto)
                .toList();
    }

    public List<ZoneDto> getZones(Long marketId, User currentUser) {
        getAccessibleMarket(marketId, currentUser); // 접근 권한 검증(본인 담당 시장 아니면 403)

        return zoneRepository.findByMarketId(marketId).stream()
                .map(this::toZoneDto)
                .toList();
    }

    /**
     * 2026-07-25 추가: 지도에 통로를 선으로 그리고 클릭으로 정책을 지정할 수 있도록
     * 구역 간 인접(통로) 목록을 반환한다. 폐쇄된 통로도 화면에 "폐쇄됨" 표시가 필요하니
     * isActive와 무관하게 전부 반환한다(findByMarketId, findByMarketIdAndIsActiveTrue 아님).
     */
    public List<ZoneAdjacencyDto> getCorridors(Long marketId) {
        return zoneAdjacencyRepository.findByMarketId(marketId).stream()
                .map(this::toAdjacencyDto)
                .toList();
    }

    /**
     * 2026-07-25 추가: 지도에 게이트(출입구) 아이콘을 표시하고 클릭으로 열림/닫힘을
     * 토글할 수 있도록, facility_type='GATE'인 시설 목록을 반환한다.
     */
    public List<GateDto> getGates(Long marketId) {
        return facilityRepository.findByMarketId(marketId).stream()
                .filter(f -> "GATE".equalsIgnoreCase(f.getFacilityType()))
                .map(this::toGateDto)
                .toList();
    }

    /**
     * 2026-08-XX 추가: 지도에 상가/건물 폴리곤을 표시하기 위한 목록.
     * 시뮬레이션 계산에는 안 쓰이는 순수 표시용이라 권한 검증 없이 zones/gates와
     * 동일한 패턴으로 marketId 기준 조회만 한다.
     */
    public List<BuildingDto> getBuildings(Long marketId) {
        return buildingRepository.findByMarketId(marketId).stream()
                .map(this::toBuildingDto)
                .toList();
    }

    /**
     * marketId가 실제로 존재하고, currentUser가 그 시장에 접근 가능한지 검증한 뒤
     * Market 엔티티를 반환한다. DashboardService 등 다른 서비스에서도 동일한
     * 시장 접근 권한 검증이 필요할 때 재사용한다.
     */
    public Market getAccessibleMarket(Long marketId, User currentUser) {
        Market market = marketRepository.findById(marketId)
                .orElseThrow(() -> new MarketNotFoundException(marketId));

        if (!isAdmin(currentUser) && !java.util.Objects.equals(market.getMarketCode(), currentUser.getMarketCode())) {
            throw new ForbiddenActionException("담당 시장이 아니라 조회할 수 없습니다: " + marketId);
        }

        return market;
    }

    private boolean isAdmin(User user) {
        return ADMIN_ROLE_CODE.equals(user.getRulesCode());
    }

    private MarketDto toMarketDto(Market market) {
        return new MarketDto(
                market.getMarketId(),
                market.getMarketName(),
                market.getLatitude(),
                market.getLongitude()
        );
    }

    private ZoneDto toZoneDto(Zone zone) {
        return new ZoneDto(
                zone.getZoneId(),
                zone.getMarketId(),
                zone.getZoneName(),
                zone.getPolygonCoordinates()
        );
    }

    private ZoneAdjacencyDto toAdjacencyDto(ZoneAdjacency adjacency) {
        return new ZoneAdjacencyDto(
                adjacency.getAdjacencyId(),
                adjacency.getFromZoneId(),
                adjacency.getToZoneId(),
                adjacency.getPathCoordinates(),
                adjacency.getIsActive()
        );
    }

    private GateDto toGateDto(Facility facility) {
        return new GateDto(
                facility.getFacilityId(),
                facility.getName(),
                facility.getLatitude(),
                facility.getLongitude(),
                facility.getWeight()
        );
    }

    private BuildingDto toBuildingDto(Building building) {
        return new BuildingDto(
                building.getBuildingId(),
                building.getMarketId(),
                building.getPnuCode(),
                building.getPolygonCoordinates(),
                building.getHeightM(),
                building.getHeightEstimated(),
                building.getFloors()
        );
    }
}