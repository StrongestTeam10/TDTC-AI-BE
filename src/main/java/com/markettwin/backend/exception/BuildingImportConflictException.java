package com.markettwin.backend.exception;

/**
 * 2026-08-14 추가 (건물 폴리곤 적재): 그 시장에 이미 건물 데이터가 있을 때.
 *
 * 적재는 기본적으로 거부하고, 덮어쓰려면 요청에 overwrite를 명시해야 한다.
 * 이렇게 막아둔 이유는 망원시장의 건물 데이터가 손으로 가공해 넣은 값이고
 * 저장소에 시드 SQL이 없기 때문이다 - 실수로 marketId를 잘못 넣어도 아무 일이
 * 일어나지 않아야 한다.
 */
public class BuildingImportConflictException extends RuntimeException {
    public BuildingImportConflictException(String message) {
        super(message);
    }
}
