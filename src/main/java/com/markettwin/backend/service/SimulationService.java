package com.markettwin.backend.service;

import com.markettwin.backend.client.SimulationEngineClient;
import com.markettwin.backend.dto.request.PredictRequestDto;
import com.markettwin.backend.dto.request.ScenarioRequestDto;
import com.markettwin.backend.dto.response.PredictResultDto;
import com.markettwin.backend.dto.response.ScenarioResultDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SimulationService {

    private final SimulationEngineClient simulationEngineClient;

    public ScenarioResultDto runScenario(ScenarioRequestDto request) {
        // 여기서 요청 자체를 DB에 로그로 남기는 로직 추가 가능 (감사/재현용)
        return simulationEngineClient.runScenario(request);
    }

    public PredictResultDto predict(PredictRequestDto request) {
        return simulationEngineClient.predict(request);
    }
}
