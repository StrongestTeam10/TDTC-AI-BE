package com.markettwin.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 시설 관리 화면 - 상점 위치 등록
 * marketId는 수정 대상에서 제외(다른 시장으로 옮기는 것은 이번 범위 밖 - 필요하면
 * 삭제 후 재등록).
 */
@Getter
@NoArgsConstructor
public class FacilityUpdateRequestDto {

    @NotBlank(message = "업종/시설유형은 필수입니다.")
    @Size(max = 50)
    private String facilityType;

    @NotBlank(message = "상점명은 필수입니다.")
    @Size(max = 50)
    private String name;

    @NotNull(message = "위도는 필수입니다.")
    private BigDecimal latitude;

    @NotNull(message = "경도는 필수입니다.")
    private BigDecimal longitude;

    @NotNull(message = "영업 상태는 필수입니다.")
    private Boolean isActive;

    @Size(max = 500)
    private String rmk;
}
