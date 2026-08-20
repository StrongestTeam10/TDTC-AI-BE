package com.markettwin.backend.dto.response;

/**
 * OpenStreetMap에서 찾은 시장 경계 제안.
 *
 * <b>저장하지 않는다.</b> 화면에 "이 모양이 맞나요?"를 보여주기 위한 값이고, 사용자가
 * 확인·수정한 뒤 기존 구역 저장 API로 넘어간다. OSM은 자원봉사로 만들어진 데이터라
 * 시장마다 있을 수도 없을 수도 있고 모양이 실제와 다를 수도 있어서, 사람이 한 번
 * 보는 단계를 없애지 않는다.
 *
 * @param found               OSM에 폴리곤이 있었는지. 없는 것은 오류가 아니다.
 * @param polygonCoordinates  GeoJSON Polygon 문자열([경도, 위도]). found=false면 null.
 * @param sourceName          OSM에 적힌 이름. 엉뚱한 시장을 가져왔는지 사람이 확인하는 근거.
 * @param vertexCount         꼭짓점 수.
 * @param attribution         ODbL 라이선스가 요구하는 출처 표시 문구.
 */
public record MarketBoundaryDto(
        boolean found,
        String polygonCoordinates,
        String sourceName,
        int vertexCount,
        String attribution
) {
}
