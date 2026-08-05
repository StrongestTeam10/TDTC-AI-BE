package com.markettwin.backend.controller;

import com.markettwin.backend.service.ControlSystemService;
import com.markettwin.backend.dto.response.PedestrianCoordinateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/simulation")
@RequiredArgsConstructor
public class SimulationIntegrationController {

    private final ControlSystemService controlSystemService;

    /**
     * 좌표 반환 API (전면 개방)
     * 프레임 번호를 전달받아, 인구 유입 예측 시뮬레이션 등에 필요한 보행자 좌표 DTO 전체(8개 컬럼)를 JSON으로 반환합니다.
     */
    @GetMapping("/coordinates/frame")
    public ResponseEntity<PedestrianCoordinateDto> getCoordinatesForFrame(
            @RequestParam Long clipId,
            @RequestParam Integer frameId,
            @RequestParam(required = false) Long videoId) {

        List<PedestrianCoordinateDto> results = controlSystemService.getJsonCoordinates(clipId, frameId, videoId);

        if (results == null || results.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(results.get(0));
    }
}
