package com.markettwin.backend.controller;

import com.markettwin.backend.dto.response.GateDto;
import com.markettwin.backend.dto.response.MarketDto;
import com.markettwin.backend.dto.response.ZoneAdjacencyDto;
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
}