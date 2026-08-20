package com.markettwin.backend.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.markettwin.backend.exception.MarketBoundaryException;
import com.markettwin.backend.util.GeoJsonPolygons;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OpenStreetMap(Overpass API)에서 시장 경계 폴리곤을 찾아온다.
 *
 * 왜 OSM인가: 전통시장의 <b>경계</b>를 폴리곤으로 주는 공개 API가 달리 없다. 카카오·네이버
 * 검색은 좌표 한 점만 주고, 공공데이터 전국전통시장표준데이터도 위경도뿐이다. OSM에는
 * amenity=marketplace 로 태그된 폴리곤이 있고, 인증키도 필요 없다.
 * 확인: 망원시장 way 31856777, 32점 폴리곤. 시드 데이터와 남쪽 끝이 1m 차이.
 *
 * ⚠️ 자원봉사로 만들어진 데이터라 시장마다 있을 수도 없을 수도 있고, 모양이 실제와
 *    다를 수도 있다. 그래서 이 값은 <b>제안</b>일 뿐 바로 저장하지 않는다.
 *
 * ⚠️ 라이선스는 ODbL이다. 이 데이터로 만든 결과물에는 출처 표시가 필요하다
 *    (MarketBoundaryDto.attribution).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OsmMarketBoundaryClient {

    /**
     * 시장 중심에서 이 반경 안의 marketplace를 찾는다. 시장 폴리곤은 400m를 넘기도 해서
     * (망원시장 남북 402m) 중심점이 폴리곤 한쪽에 치우쳐 있어도 걸리도록 넉넉히 잡는다.
     */
    private static final int SEARCH_RADIUS_METERS = 500;

    /** 닫힌 링이 되려면 최소 4점(삼각형 + 닫는 점)이 필요하다. */
    private static final int MIN_RING_POINTS = 4;

    /** ODbL이 요구하는 출처 표시. 결과물(지도·보고서)에 그대로 실을 수 있는 문구다. */
    private static final String ATTRIBUTION = "© OpenStreetMap contributors (ODbL)";

    /**
     * 좌표별 조회 결과 캐시. "없음"도 캐시한다 - 없는 시장을 다시 물어봐야 여전히 없다.
     *
     * 공개 Overpass 서버는 호출량 제한이 빡빡한데, 등록 화면에서는 같은 시장을 여러 번
     * 시도하게 된다(경계를 확인하고 다시 그리고 또 확인하고). 그 반복이 그대로 서버
     * 호출이 되면 금방 막힌다.
     */
    private final Map<String, CachedBoundary> cache = new ConcurrentHashMap<>();

    /** 캐시가 무한정 자라지 않도록. 시장 수를 생각하면 넉넉한 값이다. */
    private static final int MAX_CACHE_ENTRIES = 200;

    private record CachedBoundary(Optional<OsmBoundary> value, Instant expiresAt) {
    }

    private final WebClient osmWebClient;
    private final ObjectMapper objectMapper;

    @Value("${osm.overpass-urls}")
    private String overpassUrls;

    @Value("${osm.cache-minutes}")
    private long cacheMinutes;

    /** @param polygonCoordinates GeoJSON Polygon 문자열, @param name OSM에 적힌 이름 */
    public record OsmBoundary(String polygonCoordinates, String name, int vertexCount) {
        public String attribution() {
            return ATTRIBUTION;
        }
    }

    /**
     * 시장 중심 주변에서 marketplace 폴리곤을 찾는다. 없으면 비어 있는 Optional.
     *
     * 후보가 여럿이면 (1) 중심점을 품고 있는 폴리곤을 먼저 고르고, (2) 그런 게 없으면
     * 중심점에 가장 가까운 것을 고른다. 시장 중심 좌표는 카카오 장소 검색으로 그 시장을
     * 찍어서 나온 값이라, 대개 그 시장의 폴리곤 안에 들어간다.
     */
    public Optional<OsmBoundary> findBoundary(BigDecimal latitude, BigDecimal longitude) {
        double lat = latitude.doubleValue();
        double lon = longitude.doubleValue();

        String cacheKey = String.format(Locale.ROOT, "%.5f,%.5f", lat, lon);
        CachedBoundary cached = cache.get(cacheKey);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            log.debug("OSM 경계 캐시 사용: {}", cacheKey);
            return cached.value();
        }

        Optional<OsmBoundary> found = lookup(lat, lon);

        if (cache.size() >= MAX_CACHE_ENTRIES) {
            cache.clear();
        }
        cache.put(cacheKey, new CachedBoundary(found, Instant.now().plusSeconds(cacheMinutes * 60)));
        return found;
    }

    private Optional<OsmBoundary> lookup(double lat, double lon) {
        JsonNode elements = requestMarketplaces(lat, lon);

        List<OsmBoundary> candidates = new ArrayList<>();
        List<double[]> candidateCentroids = new ArrayList<>();
        List<List<double[]>> candidateRings = new ArrayList<>();

        for (JsonNode element : elements) {
            List<double[]> ring = toRing(element.path("geometry"));
            if (ring.size() < MIN_RING_POINTS) {
                continue;
            }
            String name = element.path("tags").path("name").asText("");
            candidates.add(new OsmBoundary(GeoJsonPolygons.toPolygonJson(ring), name, ring.size()));
            candidateRings.add(ring);
            candidateCentroids.add(GeoJsonPolygons.centroid(ring));
        }

        if (candidates.isEmpty()) {
            log.info("OSM에 marketplace 폴리곤이 없습니다: {}, {}", lat, lon);
            return Optional.empty();
        }

        // (1) 중심점을 품고 있는 폴리곤
        for (int i = 0; i < candidates.size(); i++) {
            if (GeoJsonPolygons.containsPoint(candidateRings.get(i), lon, lat)) {
                return Optional.of(candidates.get(i));
            }
        }

        // (2) 중심점에 가장 가까운 폴리곤
        int nearest = 0;
        double nearestDistance = Double.MAX_VALUE;
        for (int i = 0; i < candidates.size(); i++) {
            double distance = GeoJsonPolygons.distanceMeters(candidateCentroids.get(i), new double[]{lon, lat});
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = i;
            }
        }
        log.info("중심점을 품는 폴리곤이 없어 가장 가까운 것을 고릅니다: '{}' ({}m)",
                candidates.get(nearest).name(), Math.round(nearestDistance));
        return Optional.of(candidates.get(nearest));
    }

    private JsonNode requestMarketplaces(double lat, double lon) {
        // way와 relation만 찾는다. node(점)는 경계가 없어 쓸모가 없다.
        String query = String.format(Locale.ROOT,
                "[out:json][timeout:25];("
                        + "way(around:%d,%.7f,%.7f)[\"amenity\"=\"marketplace\"];"
                        + "relation(around:%d,%.7f,%.7f)[\"amenity\"=\"marketplace\"];"
                        + ");out tags geom;",
                SEARCH_RADIUS_METERS, lat, lon, SEARCH_RADIUS_METERS, lat, lon);

        // 미러를 앞에서부터 시도한다. 공개 서버는 504·429가 흔해서 하나만 보면 자주 멈춘다.
        List<String> endpoints = overpassEndpoints();
        List<String> failures = new ArrayList<>();

        for (String endpoint : endpoints) {
            try {
                String body = osmWebClient.post()
                        .uri(endpoint)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(BodyInserters.fromFormData("data", query))
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(30))
                        .block();

                // 호출량 제한에 걸리면 JSON이 아니라 HTML 오류 페이지가 온다.
                if (body == null || !body.stripLeading().startsWith("{")) {
                    failures.add(shortHost(endpoint) + ": 응답 거부");
                    continue;
                }
                return objectMapper.readTree(body).path("elements");
            } catch (Exception e) {
                failures.add(shortHost(endpoint) + ": " + e.getClass().getSimpleName());
            }
        }

        log.warn("Overpass 미러를 모두 실패했습니다: {}", failures);
        throw new MarketBoundaryException(
                "OpenStreetMap 서버가 모두 응답하지 않습니다(" + String.join(", ", failures) + "). "
                        + "잠시 뒤 다시 시도하거나, 지도에서 직접 그려 주세요.");
    }

    private List<String> overpassEndpoints() {
        return Arrays.stream(overpassUrls.split(","))
                .map(String::trim)
                .filter(url -> !url.isEmpty())
                .toList();
    }

    /** 오류 메시지에 전체 URL 대신 호스트만 실어 읽기 쉽게 한다. */
    private String shortHost(String url) {
        try {
            return URI.create(url).getHost();
        } catch (Exception e) {
            return url;
        }
    }

    /**
     * Overpass의 geometry([{lat, lon}, ...])를 GeoJSON 순서([경도, 위도])로 바꾸고 링을 닫는다.
     * relation은 geometry가 없는 경우가 많아 그냥 건너뛴다(way가 대부분이다).
     */
    private List<double[]> toRing(JsonNode geometry) {
        if (!geometry.isArray() || geometry.isEmpty()) {
            return List.of();
        }
        List<double[]> ring = new ArrayList<>();
        for (JsonNode point : geometry) {
            if (point.has("lat") && point.has("lon")) {
                ring.add(new double[]{point.get("lon").asDouble(), point.get("lat").asDouble()});
            }
        }
        return ring;
    }
}
