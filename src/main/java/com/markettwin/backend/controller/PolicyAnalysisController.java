package com.markettwin.backend.controller;

import com.markettwin.backend.client.SimulationEngineClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/policy")
@RequiredArgsConstructor
public class PolicyAnalysisController {

    private final SimulationEngineClient simulationEngineClient;

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> analyzePolicy(
            @RequestParam("policyText") String policyText,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        // 백엔드는 단순히 SIM 엔진(Python)의 LLM 파싱 결과를 프록시하여 리턴합니다.
        Object result = simulationEngineClient.analyzePolicy(policyText, file);
        return ResponseEntity.ok(result);
    }
}
