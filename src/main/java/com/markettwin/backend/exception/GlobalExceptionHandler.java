package com.markettwin.backend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    // 회원가입 관리자 승인 / 비밀번호 찾기
    // 세 예외 모두 클래스 자체는 이미 있었는데 핸들러 등록이 빠져있어서, 지금까지는
    // 아래 handleGeneralException으로 떨어져 500으로 응답되고 있었음(버그) - 의미에
    // 맞는 상태코드로 바로잡음.
    @ExceptionHandler(AccountPendingApprovalException.class)
    public ResponseEntity<Map<String, Object>> handleAccountPendingApproval(AccountPendingApprovalException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody(ex.getMessage()));
    }

    @ExceptionHandler(AccountRejectedException.class)
    public ResponseEntity<Map<String, Object>> handleAccountRejected(AccountRejectedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody(ex.getMessage()));
    }

    @ExceptionHandler(IdentityVerificationFailedException.class)
    public ResponseEntity<Map<String, Object>> handleIdentityVerificationFailed(IdentityVerificationFailedException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(ex.getMessage()));
    }

    @ExceptionHandler(InvalidOrgCodeException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidOrgCode(InvalidOrgCodeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(ex.getMessage()));
    }

    @ExceptionHandler(InvalidMarketCodeException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidMarketCode(InvalidMarketCodeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(ex.getMessage()));
    }

    // 회원관리
    @ExceptionHandler(InvalidRoleCodeException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidRoleCode(InvalidRoleCodeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(ex.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(ex.getMessage()));
    }

    @ExceptionHandler(InvalidCategoryCodeException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCategoryCode(InvalidCategoryCodeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(ex.getMessage()));
    }

    // 게시판 기능
    @ExceptionHandler(PostNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handlePostNotFound(PostNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(ex.getMessage()));
    }

    @ExceptionHandler(AttachmentNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleAttachmentNotFound(AttachmentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(ex.getMessage()));
    }

    // 시장/구역별 권한 분리
    @ExceptionHandler(MarketNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleMarketNotFound(MarketNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(ex.getMessage()));
    }

    @ExceptionHandler(ForbiddenActionException.class)
    public ResponseEntity<Map<String, Object>> handleForbiddenAction(ForbiddenActionException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody(ex.getMessage()));
    }

    // (시장 등록): 시장 코드/이름 중복. 요청 자체는 형식상 올바르고
    // 서버 상태와 충돌하는 것이라 400이 아니라 409를 쓴다.
    @ExceptionHandler(DuplicateMarketException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateMarket(DuplicateMarketException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorBody(ex.getMessage()));
    }

    // 구역 등록/수정/삭제
    @ExceptionHandler(ZoneNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleZoneNotFound(ZoneNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(ex.getMessage()));
    }

    @ExceptionHandler(InvalidZonePolygonException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidZonePolygon(InvalidZonePolygonException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(ex.getMessage()));
    }

    // 구역 이름 중복, 또는 다른 데이터가 참조 중이라 삭제 불가.
    @ExceptionHandler(ZoneInUseException.class)
    public ResponseEntity<Map<String, Object>> handleZoneInUse(ZoneInUseException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorBody(ex.getMessage()));
    }

    // 건물 폴리곤 적재
    // 브이월드 연동 실패는 사용자가 고칠 수 있는 것이 아니라 502로 보낸다
    // (SimulationEngineException과 같은 취급). 원인 파악이 되도록 로그를 남긴다.
    @ExceptionHandler(BuildingImportException.class)
    public ResponseEntity<Map<String, Object>> handleBuildingImport(BuildingImportException ex) {
        log.error("건물 폴리곤 적재 실패: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorBody(ex.getMessage()));
    }

    // (시장 경계 제안): OSM 연동 실패. "OSM에 없다"는 것은 오류가 아니라
    // found=false로 돌려주므로 여기 오지 않는다.
    @ExceptionHandler(MarketBoundaryException.class)
    public ResponseEntity<Map<String, Object>> handleMarketBoundary(MarketBoundaryException ex) {
        log.warn("시장 경계 조회 실패: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorBody(ex.getMessage()));
    }

    // 이미 건물이 있는 시장에 overwrite 없이 적재를 시도한 경우.
    @ExceptionHandler(BuildingImportConflictException.class)
    public ResponseEntity<Map<String, Object>> handleBuildingImportConflict(
            BuildingImportConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorBody(ex.getMessage()));
    }

    // CCTV 관제 구역
    @ExceptionHandler(CctvZoneNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleCctvZoneNotFound(CctvZoneNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(ex.getMessage()));
    }

    @ExceptionHandler(InvalidCctvZonePolygonException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCctvZonePolygon(InvalidCctvZonePolygonException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(ex.getMessage()));
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<Map<String, Object>> handleFileStorage(FileStorageException ex) {
        log.error("파일 저장소 처리 실패: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody(ex.getMessage()));
    }

    // 보고서 기능
    @ExceptionHandler(ReportDataException.class)
    public ResponseEntity<Map<String, Object>> handleReportData(ReportDataException ex) {
        log.warn("보고서 생성 데이터 조건 불충족: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(ex.getMessage()));
    }

    // 보고서 기능
    @ExceptionHandler(ReportGenerationConflictException.class)
    public ResponseEntity<Map<String, Object>> handleReportConflict(
            ReportGenerationConflictException ex) {
        log.warn("보고서 생성 충돌: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorBody(ex.getMessage()));
    }

    // 추가(보안 감사 BE-09): 업로드 확장자·폴더명처럼 "요청이 잘못된"
    // 경우를 400으로 돌려준다. 핸들러가 없던 동안에는 아래 handleGeneralException
    // 으로 떨어져 500 + "서버 내부 오류"로 나갔고, 사용자는 무엇을 고쳐야 하는지
    // 알 수 없었다(로그에도 서버 잘못처럼 남았다).
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(ex.getMessage()));
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

    /**
     * 없는 경로 요청은 404로 돌려준다.
     *
     * 아래 handleGeneralException이 Exception을 전부 잡기 때문에, 매핑이 없는 경로도
     * 500 "서버 내부 오류"로 나가고 ERROR 로그까지 쌓였다. 클라이언트는 서버 장애로
     * 오해하고 모니터링 알람도 잘못 울린다. 없는 주소를 부른 것은 요청 쪽 문제이므로
     * 404가 맞다.
     *
     * NoResourceFoundException은 정적 리소스가 없을 때, NoHandlerFoundException은
     * 컨트롤러 매핑이 없을 때 발생한다.
     */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<Map<String, Object>> handleNotFound(Exception ex) {
        log.debug("존재하지 않는 경로 요청: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorBody("요청한 경로를 찾을 수 없습니다."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        // 이 핸들러가 예외를 조용히 삼켜서 콘솔에 아무 로그도 안 남는
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
}
