package com.markettwin.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 2026-08-14 추가 (시장 영역을 선으로 잘라 구역 나누기).
 *
 * 구역을 하나씩 따로 그리는 대신, 시장 영역 폴리곤을 한 번 그리고 그 위에 선을
 * 그어 나눈다. 실제 시장 구조가 그렇기 때문이다 - 망원시장도 골목 하나를 출입구
 * 위치에서 두 번 잘라 남측·중앙·북측 3구역이 됐다.
 *
 * 구역 하나만 만들 때는 이 API가 아니라 POST /api/markets/{id}/zones를 쓴다.
 */
@Getter
@NoArgsConstructor
public class ZoneSplitRequestDto {

    /** 자를 시장 영역. GeoJSON Polygon 문자열. */
    @NotBlank(message = "지도에서 시장 영역을 그려주세요.")
    private String polygonCoordinates;

    /**
     * 자르는 선들. 각 원소가 GeoJSON LineString 문자열이다. 선 N개를 그으면
     * 구역 N+1개가 나온다. 선은 무한 직선으로 취급하므로 영역을 가로지르는
     * 짧은 획만 그어도 된다.
     */
    @NotEmpty(message = "자르는 선을 하나 이상 그어주세요.")
    private List<String> cutLines;

    /**
     * 구역 이름들. 선택값이며, 주면 <b>북쪽에서 남쪽 순서</b>로 적용된다
     * (망원시장의 북측/중앙/남측과 같은 순서). 개수가 잘린 조각 수와 다르면
     * 몇 개가 나왔는지 알려주고 거부한다. 안 주면 "구역 1", "구역 2"로 자동 부여.
     */
    private List<String> zoneNames;
}
