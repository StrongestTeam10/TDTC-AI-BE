package com.markettwin.backend.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * (시장 등록).
 *
 * 지금까지 시장은 seed-market-data.sql로만 들어갔다. 등록 API가 없으니 새 시장을
 * 추가하려면 SQL을 직접 쳐야 했고, 시장이 없으면 구역도 CCTV도 만들 수 없어서
 * 신규 시장 온보딩 전체가 막혀 있었다.
 *
 * marketCode를 필수로 받는 이유: mrkaddr01m.market_code에 DEFAULT 'MKTMW'가 걸려
 * 있다. 코드를 안 주면 새로 만든 시장에 망원시장 코드가 그대로 박히고,
 * MarketService.getAccessibleMarket이 market_code로 담당 시장을 판정하므로
 * 망원시장 담당자가 남의 시장을 조회·수정할 수 있게 된다. 예외도 안 나고 조용히
 * 뚫리는 종류라 요청 단계에서 막는다.
 */
@Getter
@NoArgsConstructor
public class MarketCreateRequestDto {

    @NotBlank(message = "시장 이름은 필수입니다.")
    @Size(max = 50, message = "시장 이름은 50자를 넘을 수 없습니다.")
    private String marketName;

    /**
     * comcode01m MKT 도메인 코드. 기존 값(MKTMW 망원시장, MKTHD 해운대시장)과 형식을
     * 맞춰 'MKT' + 영문 대문자/숫자 2자로 고정한다. comcode01m.code가 VARCHAR(5)라
     * 5자를 넘을 수 없다.
     */
    @NotBlank(message = "시장 코드는 필수입니다.")
    @Pattern(
            regexp = "^MKT[A-Z0-9]{2}$",
            message = "시장 코드는 MKT로 시작하는 5자여야 합니다(예: MKTGN)."
    )
    private String marketCode;

    @NotNull(message = "위도는 필수입니다.")
    @DecimalMin(value = "-90.0", message = "위도는 -90 ~ 90 사이여야 합니다.")
    @DecimalMax(value = "90.0", message = "위도는 -90 ~ 90 사이여야 합니다.")
    private BigDecimal latitude;

    @NotNull(message = "경도는 필수입니다.")
    @DecimalMin(value = "-180.0", message = "경도는 -180 ~ 180 사이여야 합니다.")
    @DecimalMax(value = "180.0", message = "경도는 -180 ~ 180 사이여야 합니다.")
    private BigDecimal longitude;
}
