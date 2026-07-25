package com.markettwin.backend.service;

import com.markettwin.backend.domain.entity.Market;
import com.markettwin.backend.domain.entity.Zone;
import com.markettwin.backend.domain.entity.ZoneAdjacency;
import com.markettwin.backend.dto.response.MarketDto;
import com.markettwin.backend.dto.response.ZoneAdjacencyDto;
import com.markettwin.backend.dto.response.ZoneDto;
import com.markettwin.backend.repository.MarketRepository;
import com.markettwin.backend.repository.ZoneAdjacencyRepository;
import com.markettwin.backend.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MarketService {

    private final MarketRepository marketRepository;
    private final ZoneRepository zoneRepository;
    private final ZoneAdjacencyRepository zoneAdjacencyRepository;

    public List<MarketDto> getMarkets() {
        return marketRepository.findAll().stream()
                .map(this::toMarketDto)
                .toList();
    }

    public List<ZoneDto> getZones(Long marketId) {
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
}