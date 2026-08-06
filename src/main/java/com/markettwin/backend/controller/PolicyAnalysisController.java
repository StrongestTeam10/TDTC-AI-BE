package com.markettwin.backend.controller;

import com.markettwin.backend.client.SimulationEngineClient;
import com.markettwin.backend.dto.request.PolicyAnalysisRequestDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/policy")
@RequiredArgsConstructor
public class PolicyAnalysisController {

    private final SimulationEngineClient simulationEngineClient;

    @PostMapping("/analyze")
    public ResponseEntity<Object> analyzePolicy(@Valid @RequestBody PolicyAnalysisRequestDto request) {
        // 백엔드는 단순히 SIM 엔진(Python)의 LLM 파싱 결과를 프록시하여 리턴합니다.
        Object result = simulationEngineClient.analyzePolicy(request);
        return ResponseEntity.ok(result);
    }
}
