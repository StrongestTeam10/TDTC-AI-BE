package com.markettwin.backend.exception;

/**
 * 보고서 기능
 *
 * 같은 시나리오의 보고서 생성이 동시에 진행돼, 다른 요청이 먼저 결과를 기록했을 때 던진다.
 *
 * 보고서 생성은 SIM 호출까지 수 분이 걸려 트랜잭션으로 묶지 않는다. 그래서 두 요청이
 * 겹치면 각자 파일을 올린 뒤 마지막 갱신만 DB에 남고 나머지는 참조 없는 객체가 된다.
 * 뒤늦게 갱신을 시도한 요청은 자기가 올린 파일을 지우고 이 예외를 던져, 저장소에
 * 참조 없는 파일이 남지 않게 한다.
 *
 * GlobalExceptionHandler가 409로 변환한다.
 */
public class ReportGenerationConflictException extends RuntimeException {
    public ReportGenerationConflictException(String message) {
        super(message);
    }
}
