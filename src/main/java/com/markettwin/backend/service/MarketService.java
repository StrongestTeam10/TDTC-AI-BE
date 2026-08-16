package com.markettwin.backend.service;

import com.markettwin.backend.client.OsmMarketBoundaryClient;
import com.markettwin.backend.domain.entity.Building;
import com.markettwin.backend.domain.entity.CommonCode;
import com.markettwin.backend.domain.entity.CommonCodeId;
import com.markettwin.backend.domain.entity.Facility;
import com.markettwin.backend.domain.entity.User;
import com.markettwin.backend.domain.entity.Market;
import com.markettwin.backend.domain.entity.Zone;
import com.markettwin.backend.domain.entity.ZoneAdjacency;
import com.markettwin.backend.dto.request.MarketCreateRequestDto;
import com.markettwin.backend.dto.request.MarketUpdateRequestDto;
import com.markettwin.backend.dto.response.BuildingDto;
import com.markettwin.backend.dto.response.GateDto;
import com.markettwin.backend.dto.response.MarketBoundaryDto;
import com.markettwin.backend.dto.response.MarketDto;
import com.markettwin.backend.dto.response.ZoneAdjacencyDto;
import com.markettwin.backend.dto.response.ZoneDto;
import com.markettwin.backend.repository.BuildingRepository;
import com.markettwin.backend.repository.CommonCodeRepository;
import com.markettwin.backend.repository.FacilityRepository;
import com.markettwin.backend.exception.DuplicateMarketException;
import com.markettwin.backend.exception.ForbiddenActionException;
import com.markettwin.backend.exception.MarketBoundaryException;
import com.markettwin.backend.exception.MarketNotFoundException;
import com.markettwin.backend.repository.MarketRepository;
import com.markettwin.backend.repository.ZoneAdjacencyRepository;
import com.markettwin.backend.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

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
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketService {

    private static final String ADMIN_ROLE_CODE = "ROL01";

    // 2026-08-14 추가 (시장 등록): comcode01m의 담당 시장 도메인.
    private static final String MARKET_CODE_DOMAIN = "MKT";

    private final MarketRepository marketRepository;
    private final ZoneRepository zoneRepository;
    private final ZoneAdjacencyRepository zoneAdjacencyRepository;
    private final FacilityRepository facilityRepository;
    private final BuildingRepository buildingRepository;
    private final CommonCodeRepository commonCodeRepository;
    private final OsmMarketBoundaryClient osmMarketBoundaryClient;

    /**
     * 2026-08-14 추가 (시장 등록). 관리자(ROL01)만 호출할 수 있다.
     *
     * comcode01m(MKT)과 mrkaddr01m에 한 트랜잭션으로 함께 넣는다. 시장은 두 곳에
     * 나뉘어 존재하기 때문이다 - 회원가입·게시판의 시장 선택은 공통코드(MKT)에서
     * 오고, 좌표·구역·CCTV는 mrkaddr01m의 market_id로 이어진다. 한쪽만 들어가면
     * "목록에는 보이는데 구역을 못 만드는 시장" 또는 "구역은 있는데 아무 화면에도
     * 안 뜨는 시장"이 생긴다.
     *
     * 등록만 있고 수정·삭제가 없는 이유: mrkaddr01d(구역), mrkfcts01m(시설),
     * mrkcctv01m, mrkbldg01m, simscnr01m이 전부 market_id를 참조한다. 삭제는
     * 연쇄 영향이 커서 이번 범위에서 뺐다.
     */
    @Transactional
    public MarketDto createMarket(MarketCreateRequestDto request, User currentUser) {
        assertCanManageMarkets(currentUser);

        String marketCode = request.getMarketCode().trim().toUpperCase(Locale.ROOT);
        String marketName = request.getMarketName().trim();

        if (commonCodeRepository.existsByCodeCobAndCode(MARKET_CODE_DOMAIN, marketCode)) {
            throw new DuplicateMarketException("이미 쓰이고 있는 시장 코드입니다: " + marketCode);
        }
        // comcode01m.code_name은 도메인별이 아니라 테이블 전체에 UNIQUE라, 미리 막지
        // 않으면 DataIntegrityViolationException이 500으로 새어 나간다.
        if (commonCodeRepository.existsByCodeName(marketName)) {
            throw new DuplicateMarketException("이미 쓰이고 있는 이름입니다: " + marketName);
        }

        commonCodeRepository.save(CommonCode.builder()
                .codeCob(MARKET_CODE_DOMAIN)
                .code(marketCode)
                .codeName(marketName)
                .describe("usrusrs01m.market_code / brdpsts01m.market_code - 담당 시장 구분")
                .remark("")
                .build());

        Market saved = marketRepository.save(Market.builder()
                .marketName(marketName)
                .marketCode(marketCode)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build());

        return toMarketDto(saved);
    }

    /**
     * 2026-08-14 추가 (시장 정보 수정). 관리자(ROL01)만.
     *
     * 이름과 좌표만 바꾼다. 코드는 바꾸지 않는다 - 이유는 MarketUpdateRequestDto 주석 참고.
     *
     * 이름을 바꿀 때 comcode01m(MKT)의 code_name도 함께 갱신한다. 두 곳이 갈라지면
     * 회원가입·게시판의 시장 선택 드롭다운은 옛 이름을, 지도와 보고서는 새 이름을
     * 보여주게 된다.
     */
    @Transactional
    public MarketDto updateMarket(Long marketId, MarketUpdateRequestDto request, User currentUser) {
        assertCanManageMarkets(currentUser);
        Market market = getAccessibleMarket(marketId, currentUser);

        String newName = request.getMarketName().trim();
        boolean nameChanged = !newName.equals(market.getMarketName().trim());

        if (nameChanged) {
            if (commonCodeRepository.existsByCodeName(newName)) {
                throw new DuplicateMarketException("이미 쓰이고 있는 이름입니다: " + newName);
            }
            renameCommonCode(market.getMarketCode(), newName);
        }

        // getAccessibleMarket이 돌려준 관리 상태 엔티티를 그대로 고친다.
        // 더티 체킹이 UPDATE를 내보내므로 save() 호출이 필요 없고, 코드 컬럼은 손대지 않는다.
        market.updateInfo(newName, request.getLatitude(), request.getLongitude());

        return toMarketDto(market);
    }

    /**
     * 공통코드의 표시 이름을 바꾼다.
     *
     * 코드 행이 없는 경우(마이그레이션 전에 SQL로 직접 넣은 시장)에는 경고만 남기고
     * 넘어간다. 시장 자체의 이름 변경까지 막을 이유는 없고, 막으면 옛 데이터를
     * 영영 고칠 수 없게 된다.
     */
    private void renameCommonCode(String marketCode, String newName) {
        commonCodeRepository.findById(new CommonCodeId(MARKET_CODE_DOMAIN, marketCode))
                .ifPresentOrElse(
                        // 트랜잭션 안에서 조회한 관리 상태 엔티티라 더티 체킹으로 UPDATE된다.
                        existing -> existing.rename(newName),
                        () -> log.warn("공통코드 {}/{}가 없어 표시 이름을 갱신하지 못했습니다.",
                                MARKET_CODE_DOMAIN, marketCode));
    }

    /**
     * 2026-08-14 추가: OpenStreetMap에서 이 시장의 경계 폴리곤을 찾아 <b>제안</b>한다.
     * 저장하지 않는다 - 화면이 보여주고, 사람이 확인·수정한 뒤 구역 저장 API로 넘어간다.
     *
     * 전통시장 경계를 폴리곤으로 주는 공개 API가 달리 없어서 OSM을 쓴다(카카오·네이버
     * 검색과 공공데이터 전국전통시장표준데이터는 모두 좌표 한 점만 준다). 자원봉사
     * 데이터라 없는 시장도 많고, 그때는 found=false로 돌려주고 직접 그리게 한다.
     */
    public MarketBoundaryDto suggestBoundary(Long marketId, User currentUser) {
        assertCanManageMarkets(currentUser);
        Market market = getAccessibleMarket(marketId, currentUser);

        if (market.getLatitude() == null || market.getLongitude() == null) {
            throw new MarketBoundaryException(
                    "시장 중심 좌표가 없어 경계를 찾을 수 없습니다: " + market.getMarketName());
        }

        return osmMarketBoundaryClient.findBoundary(market.getLatitude(), market.getLongitude())
                .map(boundary -> new MarketBoundaryDto(
                        true,
                        boundary.polygonCoordinates(),
                        boundary.name(),
                        boundary.vertexCount(),
                        boundary.attribution()))
                .orElseGet(() -> new MarketBoundaryDto(false, null, null, 0, null));
    }

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

    /**
     * 2026-08-14 추가 (시장 등록): 시장 추가·구역 편집 권한 검증.
     *
     * SecurityConfig는 /api/simulation/**, /api/dashboard/** 외에는
     * anyRequest().authenticated()라서, 여기서 막지 않으면 로그인만 한
     * 조회자(ROL03)도 시장을 만들 수 있다. CCTV 구역(ROL01 + 상인회)과 달리
     * 관리자만 허용하는 이유는, 구역을 고치면 과거 시나리오 결과
     * (simrslt01d.max_density_zone_id)가 가리키는 대상이 바뀌기 때문이다.
     */
    void assertCanManageMarkets(User currentUser) {
        if (currentUser == null) {
            throw new ForbiddenActionException("로그인이 필요합니다.");
        }
        if (!isAdmin(currentUser)) {
            throw new ForbiddenActionException("시장과 구역을 관리할 권한이 없습니다.");
        }
    }

    private MarketDto toMarketDto(Market market) {
        return new MarketDto(
                market.getMarketId(),
                market.getMarketName(),
                market.getLatitude(),
                market.getLongitude(),
                market.getMarketCode()
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
                facility.getWeight(),
                // is_active가 null인 과거 데이터는 "열림"으로 간주(엔티티 기본값과 동일).
                facility.getIsActive() == null ? Boolean.TRUE : facility.getIsActive()
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