package com.markettwin.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * (건물 폴리곤 자동 적재).
 *
 * 둘 다 선택값이라 빈 본문 {} 으로도 호출할 수 있다. 기본은 "반경 150m, 덮어쓰기 안 함"이다.
 */
@Getter
@NoArgsConstructor
public class BuildingImportRequestDto {

    /**
     * 시장 중심에서 몇 m까지의 건물을 가져올지. 생략하면 150m.
     *
     * 상한을 1000m로 둔 이유: 반경이 커지면 건물 수가 제곱으로 늘어 브이월드 페이지가
     * 급격히 많아지고, 시장과 무관한 건물이 지도와 시뮬레이션 격자를 무겁게 만든다.
     */
    @Min(value = 50, message = "반경은 50m 이상이어야 합니다.")
    @Max(value = 1000, message = "반경은 1000m를 넘을 수 없습니다.")
    private Integer radiusMeters;

    /**
     * 지도에서 지정한 사각형 범위. 주면 반경 대신 이걸 쓴다.
     *
     * 시장 골목은 대개 한쪽으로 긴 모양이라, 원으로 감싸면 필요 없는 사방이 함께
     * 들어온다. 필요한 영역만 집어서 받으면 나중에 정리할 일이 줄어든다.
     */
    @Valid
    private BuildingBoundsDto bounds;

    /**
     * 이미 건물이 있는 시장에 다시 적재할지.
     *
     * 기본값(null/false)이면 기존 데이터가 있을 때 409로 거부한다. 망원시장 건물은
     * 손으로 가공해 넣은 값이고 저장소에 시드 SQL이 없어서, 실수로 marketId를 잘못
     * 넣었을 때 조용히 덮어써지면 복구할 방법이 없기 때문이다.
     */
    private Boolean overwrite;
}
