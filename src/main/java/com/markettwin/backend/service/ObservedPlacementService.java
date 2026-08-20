package com.markettwin.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.markettwin.backend.domain.entity.CctvZone;
import com.markettwin.backend.domain.entity.PedestrianCoordinateJson;
import com.markettwin.backend.domain.entity.VideoClip;
import com.markettwin.backend.domain.entity.Zone;
import com.markettwin.backend.dto.request.ObservedAgentDto;
import com.markettwin.backend.repository.CctvZoneRepository;
import com.markettwin.backend.repository.PedestrianCoordinateJsonRepository;
import com.markettwin.backend.repository.VideoClipRepository;
import com.markettwin.backend.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * CCTV 관측 기반 초기배치 조립 (BE 조립 → SIM 전달).
 *
 * 호모그래피/바운더리 없이, "화면 속 사람 위치를 구역 지도 틀에 비율대로 옮기는" 경량 방식.
 * 구역마다:
 *   1) 그 구역 최신 영상(vdoclip01m)의 "지금 시점 프레임"을 고른다(반복재생 → 시각 기반).
 *   2) 그 프레임의 사람 픽셀좌표(pedaggr01h.pixels_json)를 영상 해상도로 0~1 정규화한다.
 *   3) 그 구역 CCTV 4점 폴리곤(mrkcctv01m)에 bilinear로 매핑해 지도 좌표(lon,lat)를 만든다.
 *   4) 시뮬 구역(mrkaddr01d) 밖으로 나가면 안으로 스냅한다.
 * CCTV 구역/영상/프레임이 없는 구역은 조용히 건너뛴다(SIM에서 유입 인원으로만 채움).
 *
 * capturedAt(요청 시각)을 개입 전/후가 동일하게 주면 같은 프레임이 선택되어 배치가 일치한다.
 */
@Service
@RequiredArgsConstructor
public class ObservedPlacementService {

    private static final Logger log = LoggerFactory.getLogger(ObservedPlacementService.class);

    // 영상 해상도(정규화 기준). 데이터상 대략 1280x720. 값이 달라져도 근사이므로 무방.
    private static final double IMAGE_WIDTH = 1280.0;
    private static final double IMAGE_HEIGHT = 720.0;
    // 반복재생 프레임 선택 간격(ms). 약 30fps 가정. 정확한 fps가 아니어도 "라이브 느낌"이면 충분.
    private static final long FRAME_INTERVAL_MS = 33L;
    // (퍼뜨리기): 사람이 화면 일부에만 몰려도 CCTV 구역 전체에 자연스럽게
    // 퍼지도록, 이 수 이상이면 "그 프레임 사람들의 픽셀 범위(bbox)"로 정규화한다.
    // 겹침↓ + 출발이 흩어져 최근접 게이트가 위치별로 갈려 소시지 대피 완화
    private static final int SPREAD_MIN_PEOPLE = 3;
    // bbox 변이 이 픽셀보다 작으면(사실상 한 점) 늘리지 않는다(과장 방지).
    private static final double SPREAD_MIN_PIXELS = 20.0;

    private final ZoneRepository zoneRepository;
    private final CctvZoneRepository cctvZoneRepository;
    private final VideoClipRepository videoClipRepository;
    private final PedestrianCoordinateJsonRepository pedestrianRepository;
    private final ObjectMapper objectMapper;

    /** 시장의 각 구역에서 관측 초기 위치를 조립한다. 없으면 빈 리스트. */
    public List<ObservedAgentDto> computeForMarket(Long marketId, Instant capturedAt) {
        long ts = (capturedAt != null ? capturedAt : Instant.now()).toEpochMilli();
        List<ObservedAgentDto> result = new ArrayList<>();

        for (Zone zone : zoneRepository.findByMarketId(marketId)) {
            Long zoneId = zone.getZoneId();
            try {
                CctvZone cctv = cctvZoneRepository.findFirstByZoneIdAndIsActiveTrue(zoneId).orElse(null);
                if (cctv == null) continue; // CCTV 구역 4점 미등록 → 유입으로만

                VideoClip clip = videoClipRepository
                        .findTopByZoneIdAndIsDeletedFalseOrderByStartTimeDesc(zoneId).orElse(null);
                if (clip == null) continue; // 영상 없음

                Integer maxFrame = pedestrianRepository.findMaxFrameId(clip.getClipId());
                if (maxFrame == null || maxFrame < 1) continue;

                int frameId = (int) ((ts / FRAME_INTERVAL_MS) % maxFrame) + 1;
                // 폴백 없음 - 그 프레임 그대로 쓴다. 사람 0명(빈 프레임)이면
                // 그 구역은 관측 0명(정직하게 그 순간 실제 반영).
                PedestrianCoordinateJson frame = pedestrianRepository
                        .findFirstByClipIdAndFrameId(clip.getClipId(), frameId).orElse(null);
                if (frame == null || !isPopulated(frame.getPixelsJson())) continue;

                double[][] cctvQuad = cornersByGeography(parseRing(cctv.getPolygonCoordinates()));
                double[][] zoneRing = parseRing(zone.getPolygonCoordinates());
                addMappedPeople(result, zoneId, frame.getPixelsJson(), cctvQuad, zoneRing);
            } catch (Exception e) {
                // 한 구역이 깨져도 전체가 죽지 않게 스킵(그 구역은 유입으로만 채워짐).
                log.warn("관측 초기배치 조립 실패 (zone {}): {}", zoneId, e.getMessage());
            }
        }
        return result;
    }

    // pixels_json = {"1":[x,y], ...}(값=픽셀좌표 배열)을 매핑해 result에 추가.
    // 구버전 {"person_1":{"x":..,"y":..}} 객체 형식도 호환 지원.
    private void addMappedPeople(List<ObservedAgentDto> result, Long zoneId, String pixelsJson,
                                 double[][] quad, double[][] zoneRing) throws Exception {
        JsonNode root = objectMapper.readTree(pixelsJson);

        // 1) 사람 픽셀 수집
        List<double[]> pts = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> it = root.fields();
        while (it.hasNext()) {
            JsonNode p = it.next().getValue();
            if (p == null) continue;
            // pedaggr01h.pixels_json의 실제 형식은 {"1":[x,y], ...}(값=배열)이다.
            // 예전 코드는 {"x":..,"y":..}(객체)만 읽어 모든 사람을 건너뛰어 관측 0명이 됐다.
            // 배열([x,y])과 객체({x,y}) 둘 다 받아 호환성을 유지한다.
            double px, py;
            if (p.isArray() && p.size() >= 2) {
                px = p.get(0).asDouble();
                py = p.get(1).asDouble();
            } else if (p.has("x") && p.has("y")) {
                px = p.get("x").asDouble();
                py = p.get("y").asDouble();
            } else {
                continue;
            }
            pts.add(new double[]{px, py});
        }
        if (pts.isEmpty()) return;

        // 2) 정규화 기준: 사람이 충분히 많고 픽셀 범위가 있으면 그 bbox로(구역 전체에 퍼짐),
        //    아니면 화면 전체로(1~2명이 엉뚱하게 늘어나는 것 방지).
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (double[] q : pts) {
            minX = Math.min(minX, q[0]); maxX = Math.max(maxX, q[0]);
            minY = Math.min(minY, q[1]); maxY = Math.max(maxY, q[1]);
        }
        boolean useBbox = pts.size() >= SPREAD_MIN_PEOPLE
                && (maxX - minX) >= SPREAD_MIN_PIXELS && (maxY - minY) >= SPREAD_MIN_PIXELS;
        double ox = useBbox ? minX : 0.0, oy = useBbox ? minY : 0.0;
        double w = useBbox ? (maxX - minX) : IMAGE_WIDTH, h = useBbox ? (maxY - minY) : IMAGE_HEIGHT;

        // 3) 각 사람을 (u,v) 정규화 → CCTV 4점 quad에 매핑 → 구역 안으로 스냅
        for (double[] q : pts) {
            double u = clamp01((q[0] - ox) / w);
            double v = clamp01((q[1] - oy) / h);
            double[] lonLat = bilinear(u, v, quad); // [lon, lat]
            double[] snapped = snapIntoZone(lonLat, quad, zoneRing);
            result.add(new ObservedAgentDto(zoneId, snapped[1], snapped[0])); // (lat, lon)
        }
    }

    private boolean isPopulated(String pixelsJson) {
        if (pixelsJson == null) return false;
        String t = pixelsJson.trim();
        return t.length() > 2 && !t.equals("{}"); // "{}" (사람 0명)이 아니고 내용이 있음
    }

    /**
     * GeoJSON Polygon 문자열 → 바깥 링의 [lon,lat] 목록(닫힘점 제외).
     * mrkcctv01m / mrkaddr01d 모두 같은 형식(좌표 순서 [경도, 위도]).
     */
    private double[][] parseRing(String geojson) throws Exception {
        JsonNode root = objectMapper.readTree(geojson);
        JsonNode ring = root.get("coordinates").get(0);
        int n = ring.size();
        // 마지막 점이 첫 점과 같으면(닫힌 링) 제외.
        boolean closed = n >= 2
                && ring.get(0).get(0).asDouble() == ring.get(n - 1).get(0).asDouble()
                && ring.get(0).get(1).asDouble() == ring.get(n - 1).get(1).asDouble();
        int count = closed ? n - 1 : n;
        double[][] pts = new double[count][2];
        for (int i = 0; i < count; i++) {
            pts[i][0] = ring.get(i).get(0).asDouble(); // lon
            pts[i][1] = ring.get(i).get(1).asDouble(); // lat
        }
        return pts;
    }

    /**
     * 4점을 화면 축에 맞춰 배정한다: 화면 위(v=0)=먼쪽=북(lat 큼), 아래(v=1)=가까움=남,
     * 왼(u=0)=서(lon 작음), 오(u=1)=동. → [P00(NW), P10(NE), P01(SW), P11(SE)].
     * 카메라가 남향이라 가정한 기본 규칙. 방향이 다르면 배치가 회전/반전될 수 있어 추후 보정 대상.
     */
    private double[][] cornersByGeography(double[][] pts) {
        if (pts.length < 4) throw new IllegalArgumentException("CCTV 폴리곤은 4점이어야 합니다: " + pts.length);
        double[][] p = pts.clone();
        // lat 내림차순 정렬 → 앞 2개=먼(북), 뒤 2개=가까운(남)
        java.util.Arrays.sort(p, Comparator.comparingDouble((double[] a) -> a[1]).reversed());
        double[] far1 = p[0], far2 = p[1], near1 = p[2], near2 = p[3];
        double[] nw = far1[0] <= far2[0] ? far1 : far2;   // 먼쪽 중 서쪽
        double[] ne = far1[0] <= far2[0] ? far2 : far1;   // 먼쪽 중 동쪽
        double[] sw = near1[0] <= near2[0] ? near1 : near2;
        double[] se = near1[0] <= near2[0] ? near2 : near1;
        return new double[][]{nw, ne, sw, se}; // P00, P10, P01, P11
    }

    // (u,v) → 사각형 bilinear. quad = [P00, P10, P01, P11], 각 [lon,lat]. 반환 [lon,lat].
    private double[] bilinear(double u, double v, double[][] q) {
        double a = (1 - u) * (1 - v), b = u * (1 - v), c = (1 - u) * v, d = u * v;
        double lon = a * q[0][0] + b * q[1][0] + c * q[2][0] + d * q[3][0];
        double lat = a * q[0][1] + b * q[1][1] + c * q[2][1] + d * q[3][1];
        return new double[]{lon, lat};
    }

    /** 시뮬 구역 밖이면 CCTV 4점 중심 쪽으로 당겨 안으로 넣는다(중심은 구역 안이 보장됨). */
    private double[] snapIntoZone(double[] lonLat, double[][] quad, double[][] zoneRing) {
        if (pointInRing(zoneRing, lonLat[0], lonLat[1])) return lonLat;
        double cLon = (quad[0][0] + quad[1][0] + quad[2][0] + quad[3][0]) / 4.0;
        double cLat = (quad[0][1] + quad[1][1] + quad[2][1] + quad[3][1]) / 4.0;
        for (double t = 0.15; t <= 1.0; t += 0.15) {
            double lon = lonLat[0] + (cLon - lonLat[0]) * t;
            double lat = lonLat[1] + (cLat - lonLat[1]) * t;
            if (pointInRing(zoneRing, lon, lat)) return new double[]{lon, lat};
        }
        return new double[]{cLon, cLat};
    }

    // ray-casting point-in-polygon. ring = [[lon,lat], ...].
    private boolean pointInRing(double[][] ring, double lon, double lat) {
        boolean inside = false;
        for (int i = 0, j = ring.length - 1; i < ring.length; j = i++) {
            double xi = ring[i][0], yi = ring[i][1], xj = ring[j][0], yj = ring[j][1];
            boolean intersect = (yi > lat) != (yj > lat)
                    && lon < (xj - xi) * (lat - yi) / (yj - yi) + xi;
            if (intersect) inside = !inside;
        }
        return inside;
    }

    private double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}
