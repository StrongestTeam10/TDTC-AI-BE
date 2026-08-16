package com.markettwin.backend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

/**
 * 점과 폴리곤 사이 거리 계산을 확인한다.
 *
 * 이 값으로 "시장과 상관없는 건물"을 골라 <b>삭제</b>하므로(BuildingImportService
 * .pruneDistantBuildings) 틀리면 되돌릴 수 없는 피해가 난다. 위경도를 미터로 옮기는
 * 환산이 어긋나면 멀쩡한 건물이 지워지거나 엉뚱한 건물이 남는다.
 */
class GeoJsonPolygonsTest {

    /** 위도 37.55 부근의 작은 사각형. 경도 0.001도 ≈ 88m, 위도 0.001도 ≈ 111m. */
    private static final List<double[]> SQUARE = List.of(
            new double[]{126.900, 37.550},
            new double[]{126.901, 37.550},
            new double[]{126.901, 37.551},
            new double[]{126.900, 37.551}
    );

    @Test
    @DisplayName("폴리곤 안의 점은 거리가 0이다")
    void insidePointHasZeroDistance() {
        assertThat(GeoJsonPolygons.distanceToRingMeters(SQUARE, 126.9005, 37.5505)).isZero();
    }

    @Test
    @DisplayName("북쪽으로 위도 0.001도 떨어진 점은 약 111m다")
    void measuresDistanceNorthOfRing() {
        double distance = GeoJsonPolygons.distanceToRingMeters(SQUARE, 126.9005, 37.552);

        // 위도 1도 = 111.32km 이므로 0.001도 = 111.32m
        assertThat(distance).isEqualTo(111.32, offset(1.0));
    }

    @Test
    @DisplayName("동쪽으로 경도 0.001도 떨어진 점은 약 88m다(위도 37.55 기준)")
    void measuresDistanceEastOfRing() {
        double distance = GeoJsonPolygons.distanceToRingMeters(SQUARE, 126.902, 37.5505);

        // 경도 1도는 위도에 따라 줄어든다: 111.32km x cos(37.55°) ≈ 88.3km
        assertThat(distance).isEqualTo(88.3, offset(1.5));
    }

    @Test
    @DisplayName("꼭짓점 바깥 대각선 방향도 가장 가까운 꼭짓점까지의 거리로 잰다")
    void measuresDiagonalDistanceFromCorner() {
        // 북동쪽 모서리(126.901, 37.551)에서 대각선으로 벗어난 점
        double distance = GeoJsonPolygons.distanceToRingMeters(SQUARE, 126.902, 37.552);

        // 동쪽 88.3m, 북쪽 111.3m의 빗변 ≈ 142m
        assertThat(distance).isEqualTo(Math.hypot(88.3, 111.32), offset(2.0));
    }

    @Test
    @DisplayName("망원시장 남측 구역에서 300m 떨어진 점은 여유 거리 30m를 훌쩍 넘는다")
    void realZoneRejectsDistantPoint() {
        // seed-market-data.sql의 남측 구역 일부
        List<double[]> zone = List.of(
                new double[]{126.90642263, 37.55588766},
                new double[]{126.90651112, 37.55527706},
                new double[]{126.90626431, 37.55581716},
                new double[]{126.90616434, 37.55587891}
        );

        // 북쪽으로 약 300m 떨어진 지점
        double distance = GeoJsonPolygons.distanceToRingMeters(zone, 126.90642, 37.55858);

        assertThat(distance).isGreaterThan(250.0);
        assertThat(distance).isLessThan(350.0);
    }
}
