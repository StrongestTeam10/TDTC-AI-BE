package com.markettwin.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 2026-08-11 추가 (CCTV 관제 구역 저장). 08-11 2차: 고정 번호(zoneNo) 대신 소속
 * 시뮬레이션 구역(zoneId)을 받고, 사용/미사용(isActive)이 추가됐다. 등록(POST)과
 * 수정(PUT) 둘 다 이 DTO를 쓴다.
 */
@Getter
@NoArgsConstructor
public class CctvZoneSaveRequestDto {

    @NotNull(message = "시장은 필수입니다.")
    private Long marketId;

    @NotNull(message = "소속 시뮬레이션 구역을 선택해주세요.")
    private Long zoneId;

    /**
     * GeoJSON Polygon 문자열. 형식 검증(사각형 4점 + 소속 구역 폴리곤 안에 있는지)은
     * CctvZoneService에서 파싱해 확인한다.
     */
    @NotBlank(message = "지도에서 구역을 그려주세요.")
    private String polygonCoordinates;

    /** null이면 서비스단에서 true(사용)로 취급. */
    private Boolean isActive;

    @Size(max = 500)
    private String rmk;
}
