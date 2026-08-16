package com.markettwin.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 2026-08-14 추가 (시뮬레이션 구역 등록/수정). 등록(POST)과 수정(PUT) 둘 다 쓴다.
 *
 * polygonCoordinates를 문자열 그대로 받는 이유: 이 폴리곤이 사람이 지도에서 그린
 * 것인지, 나중에 건물 폴리곤 여백에서 자동 추출한 것인지 API는 구분할 필요가 없다.
 * 같은 엔드포인트로 둘 다 저장되게 두면 자동 추출을 붙일 때 이 API를 그대로 쓴다.
 *
 * 형식 검증(GeoJSON Polygon인지, 꼭짓점 3개 이상인지, 좌표 범위가 맞는지)은
 * ZoneService에서 파싱해 확인한다.
 */
@Getter
@NoArgsConstructor
public class ZoneSaveRequestDto {

    @NotBlank(message = "구역 이름은 필수입니다.")
    @Size(max = 50, message = "구역 이름은 50자를 넘을 수 없습니다.")
    private String zoneName;

    @NotBlank(message = "지도에서 구역을 그려주세요.")
    private String polygonCoordinates;
}
