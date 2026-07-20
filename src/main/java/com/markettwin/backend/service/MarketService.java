package com.markettwin.backend.service;

import com.markettwin.backend.domain.entity.Market;
import com.markettwin.backend.domain.entity.Zone;
import com.markettwin.backend.dto.response.MarketDto;
import com.markettwin.backend.dto.response.ZoneDto;
import com.markettwin.backend.repository.MarketRepository;
import com.markettwin.backend.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MarketService {

    private final MarketRepository marketRepository;
    private final ZoneRepository zoneRepository;

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
}
