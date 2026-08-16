package com.markettwin.backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 2026-08-14 추가 (구역에서 먼 건물 정리).
 *
 * 건물은 시장 중심 기준 반경으로 받아오기 때문에, 구역이 확정되고 나면 시장과 상관없는
 * 건물이 섞여 있다. 시뮬레이션은 구역 안만 쓰므로 계산에는 영향이 없지만 지도가 지저분해지고
 * 저장 공간도 낭비된다.
 */
@Getter
@NoArgsConstructor
public class BuildingPruneRequestDto {

    /**
     * 구역 경계에서 이 거리 안에 있으면 남긴다. 생략하면 30m.
     *
     * 0으로 두지 않는 이유: 구역 폴리곤은 골목을 따라 그린 것이라 골목에 접한 상가 건물이
     * 경계 밖으로 조금 나가 있는 경우가 흔하다. 그런 건물까지 지우면 시뮬레이션에서
     * 통로를 좁혀주던 벽이 사라진다.
     */
    @Min(value = 0, message = "여유 거리는 0m 이상이어야 합니다.")
    @Max(value = 500, message = "여유 거리는 500m를 넘을 수 없습니다.")
    private Integer bufferMeters;

    /** true면 지우지 않고 몇 개가 대상인지만 세어 돌려준다. 화면이 먼저 확인하는 용도. */
    private Boolean dryRun;
}
