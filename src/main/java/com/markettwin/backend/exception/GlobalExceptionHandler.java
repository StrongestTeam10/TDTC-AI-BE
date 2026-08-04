package com.markettwin.backend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(SimulationEngineException.class)
    public ResponseEntity<Map<String, Object>> handleSimulationEngineException(
            SimulationEngineException ex) {
        log.error("시뮬레이션 엔진 호출 실패", ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(errorBody(ex.getMessage()));
    }

    @ExceptionHandler(DuplicateLoginIdException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateLoginId(DuplicateLoginIdException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorBody(ex.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody(ex.getMessage()));
    }

    @ExceptionHandler(InvalidOrgCodeException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidOrgCode(InvalidOrgCodeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(ex.getMessage()));
    }

    @ExceptionHandler(InvalidMarketCodeException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidMarketCode(InvalidMarketCodeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(ex.getMessage()));
    }

    @ExceptionHandler(InvalidCategoryCodeException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCategoryCode(InvalidCategoryCodeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(ex.getMessage()));
    }

    // 2026-07-24 추가 (게시판 기능)
    @ExceptionHandler(PostNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handlePostNotFound(PostNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(ex.getMessage()));
    }

    @ExceptionHandler(AttachmentNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleAttachmentNotFound(AttachmentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(ex.getMessage()));
    }

    // 2026-07-27 추가 (시장/구역별 권한 분리)
    @ExceptionHandler(MarketNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleMarketNotFound(MarketNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(ex.getMessage()));
    }

    @ExceptionHandler(ForbiddenActionException.class)
    public ResponseEntity<Map<String, Object>> handleForbiddenAction(ForbiddenActionException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody(ex.getMessage()));
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<Map<String, Object>> handleFileStorage(FileStorageException ex) {
        log.error("파일 저장소 처리 실패: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("입력값 검증 실패");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorBody(message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        // 2026-07-24: 이 핸들러가 예외를 조용히 삼켜서 콘솔에 아무 로그도 안 남는
        // 문제가 있었음(디버깅 불가) -> 최소한 콘솔에는 실제 원인이 남도록 로깅 추가.
        log.error("처리되지 않은 예외 발생", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody("서버 내부 오류가 발생했습니다."));
    }

    private Map<String, Object> errorBody(String message) {
        return Map.of(
                "timestamp", Instant.now().toString(),
                "message", message
        );
    }

    // 2026-07-27 추가 (보고서 기능)
    @ExceptionHandler(ReportDataException.class)
    public ResponseEntity<Map<String, Object>> handleReportData(ReportDataException ex) {
        log.warn("보고서 생성 데이터 조건 불충족: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(ex.getMessage()));
    }
}
