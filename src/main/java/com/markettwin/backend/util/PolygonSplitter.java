package com.markettwin.backend.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 시장 영역 폴리곤 하나를 선으로 잘라 여러 구역으로 나눈다.
 *
 * 구역을 하나씩 따로 그리는 대신 "영역 한 번 그리고 선 몇 개 긋기"로 만드는 이유는
 * 실제 시장 구조가 그렇기 때문이다. 망원시장도 골목 하나를 출입구 위치에서 두 번
 * 잘라 남측·중앙·북측 3구역이 됐다(seed-market-data.sql 주석 참고). 구역 3개를
 * 각각 그리면 40개 꼭짓점을 찍어야 하지만, 이 방식이면 영역 하나 + 선 2개면 된다.
 *
 * 자르는 선은 <b>무한 직선</b>으로 취급한다. 사용자가 골목을 가로지르는 짧은
 * 획만 그어도 잘리도록 하기 위해서다. 대신 그 직선이 영역 경계와 정확히 두 번
 * 만나야 하며, 그렇지 않으면(선이 영역을 안 지나거나, 오목한 모양이라 네 번
 * 만나거나) 나누지 않고 알린다.
 *
 * 좌표는 GeoJSON 규격대로 [경도(x), 위도(y)] 순서다.
 */
public final class PolygonSplitter {

    private PolygonSplitter() {
    }

    /** 두 직선이 평행한지 판정할 때 쓰는 허용 오차. 위경도 단위라 아주 작게 잡는다. */
    private static final double EPSILON = 1e-12;

    /**
     * 분할 결과 조각 하나.
     *
     * @param ring   조각의 바깥 링([경도, 위도] 목록, 닫는 점 없음)
     * @param cutIds 이 조각의 경계를 만든 자르는 선 번호(0-based). 같은 번호를 가진
     *               두 조각은 그 선을 사이에 두고 맞닿아 있다 - 나중에 통로
     *               (mrkadjc01m)를 자동 생성할 때 인접 판정 없이 그대로 쓸 수 있다.
     */
    public record Piece(List<double[]> ring, Set<Integer> cutIds) {
    }

    /**
     * @param outerRing 자를 영역의 바깥 링
     * @param cutLines  자르는 선들. 각 원소는 {시작점, 끝점} 두 점.
     * @return 잘린 조각들. 자르는 선이 없으면 원본 하나만 담아 돌려준다.
     * @throws IllegalArgumentException 어떤 선도 영역을 둘로 나누지 못했을 때
     */
    public static List<Piece> split(List<double[]> outerRing, List<double[][]> cutLines) {
        List<Piece> pieces = new ArrayList<>();
        pieces.add(new Piece(
                new ArrayList<>(GeoJsonPolygons.withoutClosingPoint(outerRing)),
                new LinkedHashSet<>()));

        for (int cutIndex = 0; cutIndex < cutLines.size(); cutIndex++) {
            double[][] cut = cutLines.get(cutIndex);
            List<Piece> next = new ArrayList<>();
            boolean appliedAnywhere = false;

            for (Piece piece : pieces) {
                List<List<double[]>> halves = splitOnce(piece.ring(), cut);
                if (halves == null) {
                    // 이 선이 지나가지 않는 조각은 그대로 둔다.
                    next.add(piece);
                    continue;
                }
                appliedAnywhere = true;
                for (List<double[]> half : halves) {
                    Set<Integer> cutIds = new LinkedHashSet<>(piece.cutIds());
                    cutIds.add(cutIndex);
                    next.add(new Piece(half, cutIds));
                }
            }

            if (!appliedAnywhere) {
                throw new IllegalArgumentException(
                        (cutIndex + 1) + "번째 선이 영역을 나누지 못했습니다. "
                                + "선이 영역을 가로지르도록 그어 주세요.");
            }
            pieces = next;
        }

        return pieces;
    }

    /**
     * 링 하나를 직선으로 자른다. 교점이 정확히 2개일 때만 나누고, 아니면 null.
     */
    private static List<List<double[]>> splitOnce(List<double[]> ring, double[][] cut) {
        double originX = cut[0][0];
        double originY = cut[0][1];
        double dirX = cut[1][0] - originX;
        double dirY = cut[1][1] - originY;
        if (Math.abs(dirX) < EPSILON && Math.abs(dirY) < EPSILON) {
            return null; // 길이가 0인 선
        }

        int n = ring.size();
        List<Hit> hits = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            double[] a = ring.get(i);
            double[] b = ring.get((i + 1) % n);
            double edgeX = b[0] - a[0];
            double edgeY = b[1] - a[1];

            double denominator = cross(edgeX, edgeY, dirX, dirY);
            if (Math.abs(denominator) < EPSILON) {
                continue; // 변이 자르는 선과 평행
            }

            double t = -cross(a[0] - originX, a[1] - originY, dirX, dirY) / denominator;
            // [0, 1) 구간만 받는다. 꼭짓점을 정확히 지날 때 앞뒤 변에서 두 번
            // 잡히는 것을 막기 위해 끝점(t=1)은 다음 변의 t=0으로 넘긴다.
            if (t < 0.0 || t >= 1.0) {
                continue;
            }
            hits.add(new Hit(i, new double[]{a[0] + edgeX * t, a[1] + edgeY * t}));
        }

        if (hits.size() != 2) {
            return null;
        }

        Hit first = hits.get(0);
        Hit second = hits.get(1);

        List<double[]> half1 = new ArrayList<>();
        half1.add(first.point());
        for (int i = first.edgeIndex() + 1; i <= second.edgeIndex(); i++) {
            half1.add(ring.get(i % n));
        }
        half1.add(second.point());

        List<double[]> half2 = new ArrayList<>();
        half2.add(second.point());
        for (int i = second.edgeIndex() + 1; i <= first.edgeIndex() + n; i++) {
            half2.add(ring.get(i % n));
        }
        half2.add(first.point());

        // 잘린 자리가 꼭짓점과 겹쳐 삼각형도 못 되는 조각이 나오면 나누지 않는다.
        if (half1.size() < 3 || half2.size() < 3) {
            return null;
        }
        return List.of(half1, half2);
    }

    private static double cross(double ax, double ay, double bx, double by) {
        return ax * by - ay * bx;
    }

    private record Hit(int edgeIndex, double[] point) {
    }
}
