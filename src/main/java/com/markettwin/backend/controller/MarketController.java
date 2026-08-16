package com.markettwin.backend.controller;

import com.markettwin.backend.dto.request.BuildingImportRequestDto;
import com.markettwin.backend.dto.request.BuildingPruneRequestDto;
import com.markettwin.backend.dto.request.MarketCreateRequestDto;
import com.markettwin.backend.dto.request.MarketUpdateRequestDto;
import com.markettwin.backend.dto.response.BuildingImportResultDto;
import com.markettwin.backend.dto.response.BuildingPruneResultDto;
import com.markettwin.backend.dto.request.ZoneSaveRequestDto;
import com.markettwin.backend.dto.request.ZoneSplitRequestDto;
import com.markettwin.backend.dto.response.BuildingDto;
import com.markettwin.backend.dto.response.GateDto;
import com.markettwin.backend.domain.entity.User;
import com.markettwin.backend.dto.response.MarketBoundaryDto;
import com.markettwin.backend.dto.response.MarketDto;
import com.markettwin.backend.dto.response.ZoneAdjacencyDto;
import com.markettwin.backend.dto.response.ZoneDto;
import com.markettwin.backend.security.CurrentUserProvider;
import com.markettwin.backend.service.BuildingImportService;
import com.markettwin.backend.service.MarketService;
import com.markettwin.backend.service.ZoneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 2026-07-27 변경: 시장/구역별 권한 분리(관리자는 전체, 그 외는 본인 담당 시장만)
 * 적용을 위해 CurrentUserProvider로 로그인 사용자를 조회해 MarketService에 전달함.
 */
@RestController
@RequestMapping("/api/markets")
@RequiredArgsConstructor
public class MarketController {

    private final MarketService marketService;
    private final ZoneService zoneService;
    private final BuildingImportService buildingImportService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public List<MarketDto> getMarkets() {
        User currentUser = currentUserProvider.getCurrentUser();
        return marketService.getMarkets(currentUser);
    }

    /**
     * 2026-08-14 추가 (시장 등록). 관리자(ROL01) 전용이며, 권한 검증은
     * MarketService.assertCanManageMarkets에서 한다(SecurityConfig에는 이 경로에
     * 대한 역할 제한이 없어서 컨트롤러까지는 로그인한 누구나 도달한다).
     *
     * 시장 하나를 만들면 comcode01m(MKT)에도 같은 코드가 함께 생긴다.
     */
    @PostMapping
    public ResponseEntity<MarketDto> createMarket(@Valid @RequestBody MarketCreateRequestDto request) {
        User currentUser = currentUserProvider.getCurrentUser();
        MarketDto created = marketService.createMarket(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * 2026-08-14 추가 (시장 정보 수정). 관리자(ROL01) 전용.
     *
     * 이름과 좌표만 바꾼다. 시장 코드는 요청에 없고 바뀌지도 않는다 - 담당 시장 권한이
     * 그 코드로 갈리기 때문이다(MarketUpdateRequestDto 주석 참고).
     */
    @PutMapping("/{marketId}")
    public MarketDto updateMarket(
            @PathVariable Long marketId,
            @Valid @RequestBody MarketUpdateRequestDto request) {
        User currentUser = currentUserProvider.getCurrentUser();
        return marketService.updateMarket(marketId, request, currentUser);
    }

    /**
     * 2026-08-14 추가 (시장 경계 제안). 관리자(ROL01) 전용.
     *
     * OpenStreetMap에서 이 시장의 경계 폴리곤을 찾아 돌려준다. <b>저장하지 않는다</b> -
     * 화면이 그려서 보여주고, 사람이 확인·수정한 뒤 구역 저장 API로 넘어간다.
     * OSM에 없으면 found=false이며 그것은 오류가 아니다(직접 그리면 된다).
     */
    @GetMapping("/{marketId}/boundary/suggest")
    public MarketBoundaryDto suggestBoundary(@PathVariable Long marketId) {
        User currentUser = currentUserProvider.getCurrentUser();
        return marketService.suggestBoundary(marketId, currentUser);
    }

    @GetMapping("/{marketId}/zones")
    public List<ZoneDto> getZones(@PathVariable Long marketId) {
        User currentUser = currentUserProvider.getCurrentUser();
        return marketService.getZones(marketId, currentUser);
    }

    /**
     * 2026-08-14 추가 (구역 등록). 관리자(ROL01) 전용.
     *
     * 폴리곤을 문자열로 그대로 받는다. 사람이 지도에서 그린 것이든 나중에 건물
     * 폴리곤 여백에서 자동 추출한 것이든 같은 경로로 저장하기 위해서다.
     * 수정·삭제는 ZoneController(/api/zones/{zoneId})에 있다.
     */
    @PostMapping("/{marketId}/zones")
    public ResponseEntity<ZoneDto> createZone(
            @PathVariable Long marketId,
            @Valid @RequestBody ZoneSaveRequestDto request) {
        User currentUser = currentUserProvider.getCurrentUser();
        ZoneDto created = zoneService.createZone(marketId, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * 2026-08-14 추가 (영역을 선으로 잘라 구역 나누기). 관리자(ROL01) 전용.
     *
     * 시장 영역 폴리곤 하나와 자르는 선 N개를 받아 구역 N+1개를 한 번에 만든다.
     * 구역을 하나씩 그리는 위 API와 목적이 다를 뿐 저장되는 결과(mrkaddr01d 행)는
     * 똑같다. 응답은 북쪽에서 남쪽 순서다.
     */
    @PostMapping("/{marketId}/zones/split")
    public ResponseEntity<List<ZoneDto>> splitZones(
            @PathVariable Long marketId,
            @Valid @RequestBody ZoneSplitRequestDto request) {
        User currentUser = currentUserProvider.getCurrentUser();
        List<ZoneDto> created = zoneService.splitIntoZones(marketId, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * 2026-07-25 추가: 지도에 통로(구역 간 연결)를 선으로 그리고 클릭으로
     * 폐쇄/개방/일방통행 정책을 지정할 수 있도록 통로 목록을 반환한다.
     */
    @GetMapping("/{marketId}/corridors")
    public List<ZoneAdjacencyDto> getCorridors(@PathVariable Long marketId) {
        return marketService.getCorridors(marketId);
    }

    /**
     * 2026-07-25 추가: 지도에 게이트(출입구) 아이콘을 표시하고 클릭으로
     * 열림/닫힘을 토글할 수 있도록 게이트 목록을 반환한다.
     */
    @GetMapping("/{marketId}/gates")
    public List<GateDto> getGates(@PathVariable Long marketId) {
        return marketService.getGates(marketId);
    }

    /**
     * 2026-08-XX 추가: 지도에 상가/건물 폴리곤(3D 느낌의 형태)을 표시하기 위한 목록.
     */
    @GetMapping("/{marketId}/buildings")
    public List<BuildingDto> getBuildings(@PathVariable Long marketId) {
        return marketService.getBuildings(marketId);
    }

    /**
     * 2026-08-14 추가 (건물 폴리곤 자동 적재). 관리자(ROL01) 전용.
     *
     * 시장 중심에서 반경 안의 건물을 브이월드에서 받아 mrkbldg01m에 넣는다. 지도
     * 표시용이 아니라 시뮬레이션용이다 - SIM이 걸을 수 있는 영역에서 건물을 빼기
     * 때문에, 이 데이터가 있어야 밀집도와 대피 시간이 실제와 맞는다.
     *
     * ⚠️ 이미 건물이 있는 시장은 409로 거부한다. 덮어쓰려면 overwrite를 명시해야 한다.
     */
    @PostMapping("/{marketId}/buildings/import")
    public BuildingImportResultDto importBuildings(
            @PathVariable Long marketId,
            @Valid @RequestBody(required = false) BuildingImportRequestDto request) {
        User currentUser = currentUserProvider.getCurrentUser();
        BuildingImportRequestDto safeRequest =
                request == null ? new BuildingImportRequestDto() : request;
        return buildingImportService.importBuildings(marketId, safeRequest, currentUser);
    }

    /**
     * 2026-08-14 추가 (구역에서 먼 건물 정리). 관리자(ROL01) 전용.
     *
     * 건물은 시장 중심 기준 반경으로 받아오기 때문에, 구역이 확정되고 나면 시장과 상관없는
     * 건물이 섞여 있다. 구역 경계에서 여유 거리(기본 30m)를 벗어난 건물을 지운다.
     *
     * dryRun을 생략하거나 true로 두면 <b>세기만 하고 지우지 않는다</b>. 되돌릴 수 없는
     * 작업이라 화면이 숫자를 먼저 보여주고, 사람이 확인한 뒤 dryRun=false로 다시 부른다.
     */
    @PostMapping("/{marketId}/buildings/prune")
    public BuildingPruneResultDto pruneBuildings(
            @PathVariable Long marketId,
            @Valid @RequestBody(required = false) BuildingPruneRequestDto request) {
        User currentUser = currentUserProvider.getCurrentUser();
        BuildingPruneRequestDto safeRequest =
                request == null ? new BuildingPruneRequestDto() : request;
        return buildingImportService.pruneDistantBuildings(marketId, safeRequest, currentUser);
    }
}