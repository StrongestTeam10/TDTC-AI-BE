package com.markettwin.backend.controller;

import com.markettwin.backend.dto.request.PredictRequestDto;
import com.markettwin.backend.dto.request.ScenarioRequestDto;
import com.markettwin.backend.dto.response.PredictResultDto;
import com.markettwin.backend.dto.response.ScenarioResultDto;
import com.markettwin.backend.service.SimulationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/simulation")
@RequiredArgsConstructor
public class SimulationController {

    private final SimulationService simulationService;

    @PostMapping("/run")
    public ScenarioResultDto runScenario(@Valid @RequestBody ScenarioRequestDto request) {
        return simulationService.runScenario(request);
    }

    @PostMapping("/predict")
    public PredictResultDto predict(@Valid @RequestBody PredictRequestDto request) {
        return simulationService.predict(request);
    }
}
