package com.markettwin.backend.controlsystemCCTV.controller;

import com.markettwin.backend.controlsystemCCTV.entity.EmergencyAlert;
import com.markettwin.backend.controlsystemCCTV.service.ControlSystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.markettwin.backend.controlsystemCCTV.entity.PedestrianCoordinateJson;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiIntegrationController {

    private final ControlSystemService controlSystemService;

    @GetMapping("/alerts/unresolved")
    public List<EmergencyAlert> getUnresolvedAlerts() {
        return controlSystemService.getUnresolvedAlerts();
    }

    @GetMapping("/coordinates-json")
    public List<PedestrianCoordinateJson> getJsonCoordinates(
            @RequestParam Long clipId,
            @RequestParam Integer frameId,
            @RequestParam(required = false) Integer videoId) {

        return controlSystemService.getJsonCoordinates(clipId, frameId, videoId);
    }

}

