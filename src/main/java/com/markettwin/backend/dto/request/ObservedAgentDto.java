package com.markettwin.backend.dto.request;

/**
 * 2026-08-12 추가: CCTV 관측 기반 초기배치용 에이전트 1명의 지도 좌표.
 *
 * BE가 각 구역의 현재 프레임 사람 픽셀좌표(pedaggr01h.pixels_json)를 그 구역의
 * CCTV 4점 폴리곤(mrkcctv01m)에 비례 매핑(bilinear)해 만든 결과다. 호모그래피
 * 캘리브레이션 없이 "화면 속 상대 위치를 구역 지도 틀에 비율대로 옮긴" 근사값.
 *
 * SIM(PredictRequest/ScenarioRequest.observedAgents)이 이 좌표를 로컬 미터로
 * 변환해 초기 배치에 쓴다. 개입 전/후 양쪽에 같은 값이 주입된다.
 */
public record ObservedAgentDto(
        Long zoneId,
        Double latitude,
        Double longitude
) {
}
