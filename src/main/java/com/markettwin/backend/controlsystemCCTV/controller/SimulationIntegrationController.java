package com.markettwin.backend.controlsystemCCTV.controller;

import com.markettwin.backend.controlsystemCCTV.entity.PedestrianCoordinateJson;
import com.markettwin.backend.controlsystemCCTV.service.ControlSystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/simulation")
@RequiredArgsConstructor
public class SimulationIntegrationController {

    private final ControlSystemService controlSystemService;

    /**
     * 좌표 반환 API
     * 프레임 번호를 전달받아, 해당 프레임의 시간(captured_at), 픽셀 좌표, 3D 물리 좌표를 1줄의 JSON으로 반환합니다.
     */
    @GetMapping("/coordinates/frame")
    public ResponseEntity<Map<String, Object>> getCoordinatesForFrame(
            @RequestParam Long zoneId,
            @RequestParam Integer frameId,
            @RequestParam(defaultValue = "LIVE") String analysisMode,
            @RequestParam(required = false) Long videoId) {

        List<PedestrianCoordinateJson> results = controlSystemService.getJsonCoordinates(zoneId, frameId, analysisMode, videoId);

        if (results == null || results.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        PedestrianCoordinateJson row = results.get(0);

        Map<String, Object> response = new HashMap<>();
        response.put("captured_at", row.getCapturedAt());
        response.put("pixels_json", row.getPixelsJson());
        response.put("bev_xyz_json", row.getBevXyzJson());

        return ResponseEntity.ok(response);
    }
}
