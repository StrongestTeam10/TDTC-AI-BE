package com.markettwin.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 2026-08-14 추가: 건물을 받아올 사각형 범위.
 *
 * 반경(radiusMeters)만으로는 시장과 상관없는 건물이 잔뜩 딸려온다. 시장 골목은 대개
 * 한쪽으로 긴 모양이라 원으로 감싸면 필요 없는 사방이 함께 들어오기 때문이다
 * (광장시장 반경 150m 조회에서 실제로 확인). 지도에서 필요한 영역만 지정할 수 있게 한다.
 */
@Getter
@NoArgsConstructor
public class BuildingBoundsDto {

    @NotNull(message = "남서쪽 위도는 필수입니다.")
    private BigDecimal minLatitude;

    @NotNull(message = "남서쪽 경도는 필수입니다.")
    private BigDecimal minLongitude;

    @NotNull(message = "북동쪽 위도는 필수입니다.")
    private BigDecimal maxLatitude;

    @NotNull(message = "북동쪽 경도는 필수입니다.")
    private BigDecimal maxLongitude;
}
