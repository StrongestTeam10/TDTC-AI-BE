package com.markettwin.backend.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * (시장 정보 수정).
 *
 * marketCode가 없는 것은 일부러다. 코드는 시장의 신원이라 바뀌면 안 된다 -
 * usrusrs01m.market_code(담당 시장)와 brdpsts01m.market_code(게시글 소속)가 코드로
 * 시장을 가리키고, MarketService.getAccessibleMarket이 이 코드로 접근 권한을 판정한다.
 * 코드를 바꾸면 그 시장 담당자가 갑자기 자기 시장에 못 들어가게 된다.
 *
 * 이름과 좌표는 표시·조회용이라 바꿔도 안전하다. 다만 이름은 comcode01m.code_name과
 * 같이 움직여야 하므로 MarketService가 두 테이블을 함께 갱신한다.
 */
@Getter
@NoArgsConstructor
public class MarketUpdateRequestDto {

    @NotBlank(message = "시장 이름은 필수입니다.")
    @Size(max = 50, message = "시장 이름은 50자를 넘을 수 없습니다.")
    private String marketName;

    @NotNull(message = "위도는 필수입니다.")
    @DecimalMin(value = "-90.0", message = "위도는 -90 ~ 90 사이여야 합니다.")
    @DecimalMax(value = "90.0", message = "위도는 -90 ~ 90 사이여야 합니다.")
    private BigDecimal latitude;

    @NotNull(message = "경도는 필수입니다.")
    @DecimalMin(value = "-180.0", message = "경도는 -180 ~ 180 사이여야 합니다.")
    @DecimalMax(value = "180.0", message = "경도는 -180 ~ 180 사이여야 합니다.")
    private BigDecimal longitude;
}
