package com.markettwin.backend.dto.response;

/**
 * 지도에 게이트(출입구) 아이콘을 표시하고 클릭으로
 * 열림/닫힘을 토글할 수 있도록, mrkfcts01m 중 facility_type='GATE' 행을
 * 그대로 프론트에 내려주는 DTO.
 *
 * isActive(개폐 상태). 시장 구조 등록에서 저장한 현행 개폐를
 * 시뮬레이션 개입 전(Before)에 자동 반영하려면 조회 시점에 개폐 상태가 필요하다.
 * mrkfcts01m.is_active — GATE 행에서는 열림=true / 닫힘=false 를 뜻함
 */
public record GateDto(
        Long facilityId,
        String name,
        java.math.BigDecimal latitude,
        java.math.BigDecimal longitude,
        Double weight,
        Boolean isActive
) {
}