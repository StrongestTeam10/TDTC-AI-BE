package com.markettwin.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 2026-08-04 추가 (시설 관리 화면 - 상점 위치 등록)
 * FE FacilityMapPage의 등록 폼과 대응.
 */
@Getter
@NoArgsConstructor
public class FacilityCreateRequestDto {

    @NotNull(message = "시장은 필수입니다.")
    private Long marketId;

    @NotBlank(message = "업종/시설유형은 필수입니다.")
    @Size(max = 50)
    private String facilityType;

    @NotBlank(message = "상점명은 필수입니다.")
    @Size(max = 50)
    private String name;

    @NotNull(message = "지도에서 위치를 클릭해주세요.")
    private BigDecimal latitude;

    @NotNull(message = "지도에서 위치를 클릭해주세요.")
    private BigDecimal longitude;

    // null이면 서비스단에서 true로 취급
    private Boolean isActive;

    // 층/위치 메모를 포함한 비고. 선택 입력.
    @Size(max = 500)
    private String rmk;
}
