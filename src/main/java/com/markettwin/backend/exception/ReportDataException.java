package com.markettwin.backend.exception;

/**
 * 2026-07-27 추가 (보고서 기능)
 *
 * 보고서를 만들 재료가 DB에 갖춰지지 않았을 때 던진다.
 * (시나리오·결과·시장·현행안 누락 등)
 *
 * SIM에 보내기 전에 BE가 먼저 걸러내기 위한 예외다. 그냥 보내면 SIM이 400으로 거절하는데
 * 그 메시지만으로는 어느 데이터가 비었는지 알기 어려워, 무엇이 없는지 짚어 알려준다.
 * GlobalExceptionHandler가 400으로 변환한다.
 */
public class ReportDataException extends RuntimeException {
    public ReportDataException(String message) {
        super(message);
    }
}
