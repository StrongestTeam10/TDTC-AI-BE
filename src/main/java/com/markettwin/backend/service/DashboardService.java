package com.markettwin.backend.service;

import com.markettwin.backend.domain.entity.CrowdDensity;
import com.markettwin.backend.domain.entity.Risk;
import com.markettwin.backend.dto.response.AgentStateDto;
import com.markettwin.backend.dto.response.CrowdDensityDto;
import com.markettwin.backend.dto.response.DashboardSnapshotDto;
import com.markettwin.backend.dto.response.RiskDto;
import com.markettwin.backend.repository.CrowdDensityRepository;
import com.markettwin.backend.repository.RiskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CrowdDensityRepository crowdDensityRepository;
    private final RiskRepository riskRepository;

    public DashboardSnapshotDto getSnapshot(Instant snapshotTime) {
        Instant targetTime = snapshotTime != null ? snapshotTime : Instant.now();

        List<CrowdDensity> densities = snapshotTime != null
                ? crowdDensityRepository.findByCapturedAt(snapshotTime)
                : crowdDensityRepository.findTop100ByOrderByCapturedAtDesc();

        List<CrowdDensityDto> crowdDensities = densities.stream()
                .map(this::toCrowdDensityDto)
                .toList();

        List<RiskDto> risks = riskRepository
                .findByDetectedAtLessThanEqualOrderByDetectedAtDesc(targetTime)
                .stream()
                .limit(50)
                .map(this::toRiskDto)
                .toList();

        // 개별 에이전트 좌표는 CRDDNST01M(구역 단위 집계)만으로는 복원 불가.
        // Vision AI 원본 트래킹 결과(개별 좌표)가 별도로 필요하므로 현재는 빈 배열.
        // 시각화에 필요하면 CRDDNST01M을 구역 중심점에 인원수만큼 점으로 흩뿌리는 방식으로 근사하거나,
        // Vision AI 원본 테이블이 ERD에 추가되어야 함 (현재 ERD엔 없음).
        List<AgentStateDto> agents = List.of();

        return new DashboardSnapshotDto(targetTime, crowdDensities, risks, agents);
    }

    public List<Instant> getAvailableTimestamps() {
        // TODO: 실제로는 CRDDNST01M의 distinct captured_at 조회 쿼리로 대체
        return List.of();
    }

    private CrowdDensityDto toCrowdDensityDto(CrowdDensity density) {
        return new CrowdDensityDto(
                density.getCrowdDensityId(),
                density.getZoneId(),
                density.getVisitorCount(),
                density.getDensityScore(),
                density.getStatusLevel(),
                density.getCapturedAt()
        );
    }

    private RiskDto toRiskDto(Risk risk) {
        return new RiskDto(
                risk.getRiskId(),
                risk.getZoneId(),
                risk.getRiskScore(),
                risk.getRiskLevel(),
                risk.getReasonCode(),
                risk.getDetectedAt()
        );
    }
}
