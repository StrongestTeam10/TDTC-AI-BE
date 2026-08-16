package com.markettwin.backend.exception;

/**
 * 2026-08-14 추가 (건물 폴리곤 적재): 브이월드 연동이 실패했을 때.
 *
 * 인증키 미설정, 호출 실패, 응답 해석 실패, 브이월드의 요청 거부가 모두 여기로 온다.
 * 우리 잘못이든 상대 잘못이든 사용자가 고칠 수 있는 게 아니라 502로 내보내고,
 * 원인을 알 수 있도록 브이월드가 준 메시지를 그대로 실어 보낸다
 * (SimulationEngineException이 시뮬레이션 엔진에 대해 쓰는 방식과 같다).
 */
public class BuildingImportException extends RuntimeException {
    public BuildingImportException(String message) {
        super(message);
    }

    public BuildingImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
