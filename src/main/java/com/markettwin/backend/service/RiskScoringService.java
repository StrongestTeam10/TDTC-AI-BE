package com.markettwin.backend.service;

import com.markettwin.backend.domain.entity.CctvDetection;
import com.markettwin.backend.dto.response.RiskScoreDto;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * 밀집도/음향/이동흐름을 종합해 위험도 스코어를 산출.
 * 실제 가중치·임계치는 캘리브레이션 결과에 맞춰 조정 필요 (현재는 placeholder 로직).
 */
@Service
public class RiskScoringService {

    private static final double DENSITY_WEIGHT = 0.5;
    private static final double ACOUSTIC_WEIGHT = 0.3;
    private static final double FLOW_WEIGHT = 0.2;

    public RiskScoreDto computeRiskScore(
            List<CctvDetection> detections,
            double acousticScore,
            double flowRateScore,
            Instant timestamp
    ) {
        double densityScore = detections.stream()
                .mapToInt(CctvDetection::getPersonCount)
                .average()
                .orElse(0.0);

        double totalScore = densityScore * DENSITY_WEIGHT
                + acousticScore * ACOUSTIC_WEIGHT
                + flowRateScore * FLOW_WEIGHT;

        String level = resolveLevel(totalScore);

        return new RiskScoreDto(
                timestamp,
                totalScore,
                level,
                new RiskScoreDto.ContributingFactors(densityScore, acousticScore, flowRateScore)
        );
    }

    private String resolveLevel(double score) {
        if (score >= 80) return "critical";
        if (score >= 60) return "high";
        if (score >= 30) return "medium";
        return "low";
    }
}
