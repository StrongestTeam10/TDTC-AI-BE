package com.markettwin.backend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;

/**
 * 시장 영역을 선으로 잘라 구역을 나누는 계산을 확인한다.
 *
 * 기하 계산은 틀려도 예외가 안 나고 "그럴듯하지만 잘못된 폴리곤"이 그대로 DB에
 * 저장된다. 지도를 눈으로 봐야 겨우 알아채는 종류라 자동 검증이 필요하다.
 * 특히 면적 보존(잘린 조각들의 넓이 합 == 원본 넓이)은 링을 잘못 이어붙이면
 * 바로 깨지는 강한 불변식이다.
 */
class PolygonSplitterTest {

    /** 망원시장 남측 구역(seed-market-data.sql). 좁고 긴 대각선 띠 모양. */
    private static final double[][] MANGWON_SOUTH_ZONE = {
            {126.90642263, 37.55588766}, {126.90633926, 37.55587179}, {126.90632158, 37.55584395},
            {126.90651112, 37.55527706}, {126.90649153, 37.555273}, {126.90644344, 37.55527808},
            {126.90626431, 37.55581716}, {126.90620768, 37.55584702}, {126.90617622, 37.55584182},
            {126.90616434, 37.55587891}, {126.9061959, 37.55588502}, {126.90620359, 37.55589589},
            {126.90641962, 37.55589589}, {126.90642263, 37.55588766}
    };

    @Test
    @DisplayName("사각형을 가로선으로 자르면 두 조각이 나오고 넓이가 절반씩이다")
    void splitsSquareInHalf() {
        List<double[]> square = ring(new double[][]{{0, 0}, {10, 0}, {10, 10}, {0, 10}});
        // List.of는 가변인자라 double[][] 하나를 넘기면 double[] 두 개로 펼쳐진다. 타입을 명시한다.
        List<double[][]> cuts = List.<double[][]>of(new double[][]{{-1, 5}, {11, 5}});

        List<PolygonSplitter.Piece> pieces = PolygonSplitter.split(square, cuts);

        assertThat(pieces).hasSize(2);
        assertThat(pieces).allSatisfy(piece -> assertThat(area(piece.ring())).isEqualTo(50.0, offset(1e-9)));
    }

    @Test
    @DisplayName("선을 두 개 그으면 구역이 세 개로 나뉜다")
    void twoCutsMakeThreeZones() {
        List<double[]> square = ring(new double[][]{{0, 0}, {30, 0}, {30, 10}, {0, 10}});
        List<double[][]> cuts = List.of(
                new double[][]{{10, -1}, {10, 11}},
                new double[][]{{20, -1}, {20, 11}});

        List<PolygonSplitter.Piece> pieces = PolygonSplitter.split(square, cuts);

        assertThat(pieces).hasSize(3);
        assertThat(pieces.stream().mapToDouble(p -> area(p.ring())).sum())
                .isEqualTo(300.0, offset(1e-9));
    }

    @Test
    @DisplayName("같은 선으로 갈라진 두 조각은 서로 맞닿은 것으로 표시된다")
    void piecesFromSameCutShareCutId() {
        List<double[]> square = ring(new double[][]{{0, 0}, {10, 0}, {10, 10}, {0, 10}});

        List<PolygonSplitter.Piece> pieces =
                PolygonSplitter.split(square, List.<double[][]>of(new double[][]{{-1, 5}, {11, 5}}));

        assertThat(pieces.get(0).cutIds()).containsExactly(0);
        assertThat(pieces.get(1).cutIds()).containsExactly(0);
    }

    @Test
    @DisplayName("영역을 지나지 않는 선은 나누지 못했다고 알린다")
    void rejectsCutThatMissesArea() {
        List<double[]> square = ring(new double[][]{{0, 0}, {10, 0}, {10, 10}, {0, 10}});
        List<double[][]> cuts = List.<double[][]>of(new double[][]{{100, 100}, {200, 100}});

        assertThatThrownBy(() -> PolygonSplitter.split(square, cuts))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1번째 선이 영역을 나누지 못했습니다");
    }

    @Test
    @DisplayName("망원시장 남측 구역처럼 좁고 긴 띠를 잘라도 넓이가 보존된다")
    void preservesAreaOnRealMarketShape() {
        List<double[]> zone = ring(MANGWON_SOUTH_ZONE);
        double originalArea = area(zone);

        // 띠를 가로지르는 동서 방향 선.
        List<double[][]> cuts = List.<double[][]>of(new double[][]{{126.9060, 37.5556}, {126.9066, 37.5556}});

        List<PolygonSplitter.Piece> pieces = PolygonSplitter.split(zone, cuts);

        assertThat(pieces).hasSize(2);
        assertThat(pieces.stream().mapToDouble(p -> area(p.ring())).sum())
                .isEqualTo(originalArea, offset(1e-15));
    }

    @Test
    @DisplayName("자르는 선이 없으면 원본 하나를 그대로 돌려준다")
    void noCutsReturnsOriginal() {
        List<double[]> square = ring(new double[][]{{0, 0}, {10, 0}, {10, 10}, {0, 10}});

        List<PolygonSplitter.Piece> pieces = PolygonSplitter.split(square, List.of());

        assertThat(pieces).hasSize(1);
        assertThat(area(pieces.get(0).ring())).isEqualTo(100.0);
    }

    private static List<double[]> ring(double[][] points) {
        List<double[]> result = new ArrayList<>();
        for (double[] point : points) {
            result.add(new double[]{point[0], point[1]});
        }
        return result;
    }

    /** 신발끈 공식. 좌표 단위 그대로의 넓이라 절대값 자체에는 의미가 없고 비교용이다. */
    private static double area(List<double[]> ring) {
        List<double[]> points = GeoJsonPolygons.withoutClosingPoint(ring);
        double sum = 0;
        int n = points.size();
        for (int i = 0; i < n; i++) {
            double[] a = points.get(i);
            double[] b = points.get((i + 1) % n);
            sum += a[0] * b[1] - b[0] * a[1];
        }
        return Math.abs(sum) / 2.0;
    }
}
