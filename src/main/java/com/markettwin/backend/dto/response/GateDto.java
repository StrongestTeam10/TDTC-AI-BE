package com.markettwin.backend.dto.response;

/**
 * 2026-07-25 추가: 지도에 게이트(출입구) 아이콘을 표시하고 클릭으로
 * 열림/닫힘을 토글할 수 있도록, mrkfcts01m 중 facility_type='GATE' 행을
 * 그대로 프론트에 내려주는 DTO.
 */
public record GateDto(
        Long facilityId,
        String name,
        java.math.BigDecimal latitude,
        java.math.BigDecimal longitude,
        Double weight
) {
}