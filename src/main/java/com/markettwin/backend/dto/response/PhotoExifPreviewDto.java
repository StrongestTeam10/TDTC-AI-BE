package com.markettwin.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 상점 외관 직접 촬영 데이터 수집 파이프라인
 *
 * 사진을 최종 저장하기 전, EXIF에서 뽑아낸 GPS/촬영일시를 FE가 지도 보정 UI에
 * 미리 채워 넣을 수 있도록 미리보기 용도로 반환한다. 이 시점엔 아직 S3 업로드도,
 * DB 저장도 하지 않는다(사용자가 보정/방향 라벨링까지 마쳐야 실제 저장 API를 호출).
 */
@Getter
@Builder
@AllArgsConstructor
public class PhotoExifPreviewDto {
    private boolean hasGps;
    private BigDecimal exifLatitude;
    private BigDecimal exifLongitude;
    private Instant capturedAt;
}
