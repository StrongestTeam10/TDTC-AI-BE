package com.markettwin.backend.exception;

/**
 * 시뮬레이션 엔진이 사용량 한도(429)를 이유로 요청을 거절했을 때.
 *
 * SimulationEngineException(502)과 분리한 이유: 502는 "엔진이 고장났다"는 뜻이라
 * 사용자가 할 수 있는 일이 없지만, 이쪽은 "지금은 한도를 넘었으니 잠시 뒤 다시"라서
 * 안내와 대응이 다르다. 화면에서도 장애가 아니라 대기 상황으로 보여줘야 한다.
 */
public class SimulationEngineRateLimitException extends RuntimeException {

    public SimulationEngineRateLimitException(String message) {
        super(message);
    }
}
