package com.markettwin.backend.dto.response;

/**
 * 2026-08-14 추가 (건물 폴리곤 자동 적재 결과).
 *
 * fetchedFeatures와 savedBuildings가 다를 수 있다. 브이월드가 건물 하나를
 * MultiPolygon(여러 조각)으로 주면 조각마다 한 행으로 저장하기 때문이다.
 * 두 숫자를 모두 내려주는 이유는, 적재가 실제로 무엇을 했는지 화면과 로그에서
 * 확인할 수 있어야 하기 때문이다.
 */
public record BuildingImportResultDto(
        Long marketId,
        /** 반경으로 받았을 때의 반경(m). 사각형 범위로 받았으면 0이다. */
        int radiusMeters,
        /** 지도에서 지정한 사각형 범위로 받았는지. */
        boolean usedBounds,
        /** 브이월드가 돌려준 건물(feature) 수 */
        int fetchedFeatures,
        /** mrkbldg01m에 실제로 넣은 행 수 */
        int savedBuildings,
        /**
         * 중복이라 넣지 않은 수. 응답 안에서 겹친 것과, 이미 DB에 있던 것을 합친 값이다.
         * pnu_code에 전체 UNIQUE 제약이 있어서 이웃 시장과 반경이 겹치면 자연스럽게 생긴다.
         */
        int skippedDuplicates,
        /** overwrite로 지운 기존 행 수 */
        int deletedBuildings,
        /** 조회한 페이지 수 */
        int pages
) {
}
