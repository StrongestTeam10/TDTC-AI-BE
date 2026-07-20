package com.markettwin.backend.controller;

import com.markettwin.backend.dto.response.MarketDto;
import com.markettwin.backend.dto.response.ZoneDto;
import com.markettwin.backend.service.MarketService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/markets")
@RequiredArgsConstructor
public class MarketController {

    private final MarketService marketService;

    @GetMapping
    public List<MarketDto> getMarkets() {
        return marketService.getMarkets();
    }

    @GetMapping("/{marketId}/zones")
    public List<ZoneDto> getZones(@PathVariable Long marketId) {
        return marketService.getZones(marketId);
    }
}
