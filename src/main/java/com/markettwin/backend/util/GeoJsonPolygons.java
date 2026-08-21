package com.markettwin.backend.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * GeoJSON Polygon 문자열을 다루는 공용 계산.
 *
 * 지금까지 이 계산은 CctvZoneService 안에 private으로만 있었다(CCTV 사각형이 소속
 * 구역 안에 있는지 검증하는 용도). 구역 등록·통로 자동 생성·CCTV 소속 구역 자동
 * 판정이 모두 같은 계산을 쓰게 되면서 한곳으로 모았다.
 *
 * ⚠️ 좌표 순서는 GeoJSON 규격대로 [경도(x), 위도(y)]다. DB(mrkaddr01d,
 * mrkcctv01m, mrkbldg01m)와 화면에 들어 있는 값이 전부 이 순서라, 뒤집어도
 * 예외는 안 나고 조용히 엉뚱한 위치를 가리킨다.
 */
public final class GeoJsonPolygons {

    private GeoJsonPolygons() {
    }

    /**
     * 위도 1도 = 약 111.32km. 경도 1도는 위도에 따라 줄어들어 cos(위도)를 곱한다.
     * 시장 규모(수백 m)에서는 이 근사로 충분하고, 정확한 측지 계산을 하려면
     * 별도 라이브러리가 필요하다.
     */
    private static final double METERS_PER_DEGREE_LAT = 111_320.0;

    /**
     * GeoJSON Polygon 문자열에서 바깥 링(첫 번째 링)을 [경도, 위도] 목록으로 꺼낸다.
     * 구멍(hole)이 있는 폴리곤은 이 프로젝트에서 쓰지 않아 무시한다.
     *
     * @throws IllegalArgumentException 형식이 GeoJSON Polygon이 아닐 때. 호출하는
     *         쪽에서 각자의 도메인 예외로 감싼다.
     */
    public static List<double[]> parseOuterRing(String polygonCoordinates, ObjectMapper objectMapper) {
        JsonNode root;
        try {
            root = objectMapper.readTree(polygonCoordinates);
        } catch (Exception e) {
            throw new IllegalArgumentException("JSON 형식이 아닙니다.");
        }

        JsonNode coordinates = root.path("coordinates");
        if (!coordinates.isArray() || coordinates.isEmpty() || !coordinates.get(0).isArray()) {
            throw new IllegalArgumentException("GeoJSON Polygon 형식이 아닙니다.");
        }

        List<double[]> ring = new ArrayList<>();
        for (JsonNode point : coordinates.get(0)) {
            if (point.isArray() && point.size() >= 2) {
                ring.add(new double[]{point.get(0).asDouble(), point.get(1).asDouble()});
            }
        }
        if (ring.isEmpty()) {
            throw new IllegalArgumentException("좌표가 비어 있습니다.");
        }
        return ring;
    }

    /**
     * GeoJSON LineString에서 양 끝점을 [경도, 위도]로 꺼낸다.
     * 구역을 자르는 선은 직선으로 취급하므로 중간 점은 쓰지 않는다.
     *
     * @throws IllegalArgumentException LineString이 아니거나 점이 2개 미만일 때
     */
    public static double[][] parseLineEndpoints(String lineCoordinates, ObjectMapper objectMapper) {
        JsonNode root;
        try {
            root = objectMapper.readTree(lineCoordinates);
        } catch (Exception e) {
            throw new IllegalArgumentException("자르는 선이 JSON 형식이 아닙니다.");
        }

        JsonNode coordinates = root.path("coordinates");
        if (!coordinates.isArray() || coordinates.size() < 2) {
            throw new IllegalArgumentException("자르는 선은 점이 2개 이상인 GeoJSON LineString이어야 합니다.");
        }

        JsonNode start = coordinates.get(0);
        JsonNode end = coordinates.get(coordinates.size() - 1);
        if (!start.isArray() || start.size() < 2 || !end.isArray() || end.size() < 2) {
            throw new IllegalArgumentException("자르는 선의 좌표 형식이 올바르지 않습니다.");
        }

        return new double[][]{
                {start.get(0).asDouble(), start.get(1).asDouble()},
                {end.get(0).asDouble(), end.get(1).asDouble()}
        };
    }

    /**
     * [경도, 위도] 링을 GeoJSON Polygon 문자열로 만든다.
     * DB(mrkaddr01d.polygon_coordinates)에 들어가는 형식과 같아야 하므로,
     * 첫 점과 같은 점을 끝에 붙여 링을 닫는다.
     */
    public static String toPolygonJson(List<double[]> ring) {
        List<double[]> open = withoutClosingPoint(ring);
        StringBuilder sb = new StringBuilder("{\"type\": \"Polygon\", \"coordinates\": [[");
        for (double[] point : open) {
            sb.append('[').append(point[0]).append(", ").append(point[1]).append("], ");
        }
        double[] firstPoint = open.get(0);
        sb.append('[').append(firstPoint[0]).append(", ").append(firstPoint[1]).append(']');
        sb.append("]]}");
        return sb.toString();
    }

    /**
     * 점이 폴리곤 안에 있는지 판정(ray casting).
     * CctvZoneService.isPointInPolygon과 동일한 판정이다.
     */
    public static boolean containsPoint(List<double[]> ring, double lon, double lat) {
        boolean inside = false;
        int n = ring.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = ring.get(i)[0], yi = ring.get(i)[1];
            double xj = ring.get(j)[0], yj = ring.get(j)[1];

            boolean intersect = ((yi > lat) != (yj > lat))
                    && (lon < (xj - xi) * (lat - yi) / (yj - yi) + xi);
            if (intersect) inside = !inside;
        }
        return inside;
    }

    /**
     * 링의 대표점([경도, 위도]). 꼭짓점 평균이라 오목한 모양에서는 폴리곤 밖으로
     * 나갈 수 있는데, 지금 쓰임(구역 간 거리 계산·라벨 위치)에는 충분하다.
     * 닫힌 링의 마지막 중복점은 평균에서 제외한다.
     */
    public static double[] centroid(List<double[]> ring) {
        List<double[]> points = withoutClosingPoint(ring);
        double lon = 0;
        double lat = 0;
        for (double[] p : points) {
            lon += p[0];
            lat += p[1];
        }
        return new double[]{lon / points.size(), lat / points.size()};
    }

    /** 닫힌 링(첫 점 == 마지막 점)이면 마지막 중복점을 뺀 목록을 돌려준다. */
    public static List<double[]> withoutClosingPoint(List<double[]> ring) {
        int n = ring.size();
        if (n >= 2 && ring.get(0)[0] == ring.get(n - 1)[0] && ring.get(0)[1] == ring.get(n - 1)[1]) {
            return ring.subList(0, n - 1);
        }
        return ring;
    }

    /**
     * 점이 링에서 얼마나 떨어져 있는지(m). 안에 있으면 0.
     *
     * 반경으로 받아온 건물 중 시장과 상관없는 것을 골라내는 데 쓴다. 시장 중심에서
     * 반경 150m로 조회하면 시장 골목과 무관한 건물까지 딸려 오는데, 그것들은
     * 시뮬레이션에서 쓰이지도 않으면서 지도만 어지럽힌다.
     *
     * 위경도를 점 기준 로컬 미터로 옮겨 평면에서 계산한다. 수백 m 범위에서는 충분하다.
     */
    public static double distanceToRingMeters(List<double[]> ring, double lon, double lat) {
        if (containsPoint(ring, lon, lat)) {
            return 0.0;
        }
        double nearest = Double.MAX_VALUE;
        int n = ring.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            nearest = Math.min(nearest, distanceToSegmentMeters(lon, lat, ring.get(j), ring.get(i)));
        }
        return nearest;
    }

    /** 점(lon, lat)에서 선분 a-b까지의 거리(m). 점을 원점으로 옮겨 평면에서 잰다. */
    private static double distanceToSegmentMeters(double lon, double lat, double[] a, double[] b) {
        double lonScale = METERS_PER_DEGREE_LAT * Math.cos(Math.toRadians(lat));

        double ax = (a[0] - lon) * lonScale;
        double ay = (a[1] - lat) * METERS_PER_DEGREE_LAT;
        double bx = (b[0] - lon) * lonScale;
        double by = (b[1] - lat) * METERS_PER_DEGREE_LAT;

        double dx = bx - ax;
        double dy = by - ay;
        double lengthSquared = dx * dx + dy * dy;
        if (lengthSquared == 0) {
            return Math.hypot(ax, ay);
        }
        // 원점(=판정하려는 점)에서 선분에 내린 수선의 발. 선분 밖이면 끝점으로 잘라낸다.
        double t = Math.max(0, Math.min(1, -(ax * dx + ay * dy) / lengthSquared));
        return Math.hypot(ax + t * dx, ay + t * dy);
    }

    /** 두 [경도, 위도] 점 사이의 대략적인 거리(m). */
    public static double distanceMeters(double[] a, double[] b) {
        double midLatRad = Math.toRadians((a[1] + b[1]) / 2.0);
        double dx = (a[0] - b[0]) * METERS_PER_DEGREE_LAT * Math.cos(midLatRad);
        double dy = (a[1] - b[1]) * METERS_PER_DEGREE_LAT;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
