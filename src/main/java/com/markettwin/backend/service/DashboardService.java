package com.markettwin.backend.service;

import com.markettwin.backend.domain.entity.AlertLog;
import com.markettwin.backend.domain.entity.CctvDetection;
import com.markettwin.backend.dto.response.AgentStateDto;
import com.markettwin.backend.dto.response.AlertLogDto;
import com.markettwin.backend.dto.response.DashboardSnapshotDto;
import com.markettwin.backend.repository.AlertLogRepository;
import com.markettwin.backend.repository.CctvDetectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CctvDetectionRepository cctvDetectionRepository;
    private final AlertLogRepository alertLogRepository;
    private final RiskScoringService riskScoringService;

    public DashboardSnapshotDto getSnapshot(Instant snapshotTime) {
        Instant targetTime = snapshotTime != null ? snapshotTime : Instant.now();

        List<CctvDetection> detections = snapshotTime != null
                ? cctvDetectionRepository.findByTimestamp(snapshotTime)
                : cctvDetectionRepository.findTop100ByOrderByTimestampDesc();

        List<AgentStateDto> agents = detections.stream()
                .map(this::toAgentState)
                .toList();

        // TODO: acousticScore, flowRateScore는 acoustic_events / lidar_readings 조회로 대체
        var riskScore = riskScoringService.computeRiskScore(detections, 0.0, 0.0, targetTime);

        List<AlertLogDto> alerts = alertLogRepository
                .findByTimestampLessThanEqualOrderByTimestampDesc(targetTime)
                .stream()
                .limit(50)
                .map(this::toAlertDto)
                .toList();

        return new DashboardSnapshotDto(targetTime, agents, riskScore, alerts);
    }

    public List<Instant> getAvailableTimestamps() {
        // TODO: 실제로는 cctv_detections 테이블의 distinct timestamp 조회 쿼리로 대체
        return List.of();
    }

    private AgentStateDto toAgentState(CctvDetection detection) {
        // CCTV 탐지 데이터를 개별 에이전트 좌표로 매핑하는 로직은
        // Vision AI의 개별 트래킹 결과(person_count 이상)에 따라 정교화 필요
        return new AgentStateDto(
                detection.getDetectionId(),
                detection.getNodeId(),
                0.0,
                0.0,
                "normal"
        );
    }

    private AlertLogDto toAlertDto(AlertLog alert) {
        return new AlertLogDto(
                alert.getAlertId(),
                alert.getTimestamp(),
                alert.getNodeId(),
                alert.getAlertType(),
                alert.getMessage(),
                alert.getResolved()
        );
    }
}
