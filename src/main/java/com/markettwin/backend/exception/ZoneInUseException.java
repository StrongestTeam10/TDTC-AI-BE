package com.markettwin.backend.exception;

/**
 * (구역 삭제): 다른 데이터가 이 구역을 참조하고 있어 지울 수 없을 때.
 *
 * mrkaddr01d(zone_id)를 참조하는 테이블이 7개다 - mrkcctv01m(CCTV 구역),
 * mrkadjc01m(통로), simrslt01d·simbsln01d(시뮬레이션 결과의 최대밀집 구역),
 * vdoclip01m(영상 클립), emgalrt01h(비상 알림), mrkrisk01m_zone_legacy.
 * 이 중 통로만 구역에서 파생된 값이라 함께 지우고, 나머지는 이력이거나 사람이
 * 등록한 데이터라 삭제를 막는다.
 */
public class ZoneInUseException extends RuntimeException {
    public ZoneInUseException(String message) {
        super(message);
    }
}
