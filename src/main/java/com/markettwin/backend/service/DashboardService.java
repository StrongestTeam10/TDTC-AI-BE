package com.markettwin.backend.service;

import com.markettwin.backend.client.SimulationEngineClient;
import com.markettwin.backend.domain.entity.User;
import com.markettwin.backend.dto.request.SnapshotRequestDto;
import com.markettwin.backend.dto.response.DashboardSnapshotDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SimulationEngineClient simulationEngineClient;
    private final MarketService marketService;

    /**
     * 파이프라인 A: SIM /simulate/snapshot을 실제로 호출해 실시간 관제 스냅샷을 받아온다.
     *
     * 2026-07-24: 기존에는 CrowdDensityRepository/RiskRepository로 DB를 직접 조회했는데,
     * 이는 실제 센서 장비가 없어 시딩 데이터 갱신 코드가 전혀 없는 상태에서 "마지막 수동
     * 호출/시딩 시점의 잔여값"을 보여주는 것에 불과했다. SIM이 매 요청마다 실제로 시뮬레이션을
     * 돌려 위험도를 산출하도록 전면 교체함.
     *
     * 2026-07-27 추가: 시장/구역별 권한 분리. 게시판과 마찬가지로 클라이언트가 보낸
     * marketId를 그대로 신뢰하지 않고, MarketService.getAccessibleMarket으로 본인
     * 담당 시장이 맞는지(관리자는 예외) 매 요청마다 서버에서 재검증한다.
     */
    public DashboardSnapshotDto getSnapshot(
            Long marketId,
            Instant capturedAt,
            Boolean persistRisk,
            Boolean includeAgents,
            User currentUser
    ) {
        marketService.getAccessibleMarket(marketId, currentUser);

        SnapshotRequestDto request = new SnapshotRequestDto(
                marketId,
                capturedAt,
                persistRisk != null ? persistRisk : Boolean.FALSE,
                includeAgents != null ? includeAgents : Boolean.TRUE
        );
        return simulationEngineClient.getSnapshot(request);
    }

    public List<Instant> getAvailableTimestamps() {
        // TODO: 파이프라인 A가 SIM 실시간 호출 방식으로 바뀌면서 이 기능의 필요성 자체가
        // 재검토 대상임. 현재는 이전 구현 그대로 빈 배열을 반환한다.
        return List.of();
    }
}
