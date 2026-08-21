package com.markettwin.backend.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.markettwin.backend.exception.BuildingImportException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 브이월드(공간정보 오픈플랫폼) 2D 데이터API에서 건물 폴리곤을 받아온다.
 *
 * 실제 응답으로 확인한 사실(망원시장 반경 조회):
 *   - geometry.type이 <b>MultiPolygon</b>이다. 그대로 저장하면 SIM parse_polygon이
 *     예외로 거부하고, FE geoJsonToVertices는 예외 없이 잘못된 좌표를 만든다.
 *     그래서 여기서 조각별 Polygon으로 풀어서 넘긴다.
 *   - 높이 필드가 없다. 지상층수(gro_flo_co)만 준다.
 *   - PNU가 없다. 대신 25자리 건물관리번호(bd_mgt_sn)를 주는데, 그 앞 19자리가 PNU다
 *     (법정동코드 10 + 대지구분 1 + 본번 4 + 부번 4).
 *   - 좌표는 요청한 대로 EPSG:4326(WGS84 경위도)로 온다. 좌표계 변환이 필요 없다.
 *   - 한 페이지 최대 1000건이고, 총 건수는 response.record.total에 온다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VworldBuildingClient {

    /** 브이월드가 허용하는 한 페이지 최대 건수. */
    private static final int PAGE_SIZE = 1000;

    /**
     * 페이지 수 상한. 응답이 이상해서 page.total이 비정상적으로 크게 와도 무한히 돌지
     * 않게 막는다. 1000건 x 20페이지면 시장 하나 주변으로는 충분하고도 남는다.
     */
    private static final int MAX_PAGES = 20;

    /** 위도 1도의 거리(m). 반경을 위경도 상자로 바꾸는 근사에 쓴다. */
    private static final double METERS_PER_DEGREE_LAT = 111_320.0;

    /**
     * 건물관리번호(bd_mgt_sn)의 최소 길이. 25자리가 정상이며,
     * 앞 19자리가 PNU(법정동 10 + 대지구분 1 + 본번 4 + 부번 4)다.
     */
    private static final int MIN_MANAGEMENT_NUMBER_LENGTH = 19;

    private final WebClient vworldWebClient;
    private final ObjectMapper objectMapper;

    @Value("${vworld.api-key}")
    private String apiKey;

    @Value("${vworld.domain}")
    private String domain;

    @Value("${vworld.building-layer}")
    private String buildingLayer;

    /**
     * @param pnuCode            mrkbldg01m.pnu_code에 넣을 값. 건물관리번호(25자리)이며,
     *                           앞 19자리가 PNU다. 조각이 여럿이면 뒤에 -1, -2가 붙는다.
     * @param polygonCoordinates GeoJSON Polygon 문자열([경도, 위도])
     * @param floors             지상층수
     */
    public record FetchedBuilding(String pnuCode, String polygonCoordinates, int floors) {
    }

    /**
     * @param buildings     저장할 수 있는 형태로 정리된 건물들
     * @param featureCount  브이월드가 돌려준 원본 건물(feature) 수
     * @param pages         조회한 페이지 수
     */
    public record FetchResult(List<FetchedBuilding> buildings, int featureCount, int pages) {
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * 시장 중심에서 반경 안의 건물을 모두 받아온다. 페이지가 여러 개면 끝까지 돈다.
     *
     * 반경 밖 건물이 상자 모서리에 조금 더 들어오는 것은 그냥 둔다. 시뮬레이션에서
     * 걸을 수 있는 영역 밖의 건물을 빼봐야 아무 일도 일어나지 않아 무해하다.
     */
    public FetchResult fetchAround(BigDecimal latitude, BigDecimal longitude, int radiusMeters) {
        return fetch(toBoxFilter(latitude.doubleValue(), longitude.doubleValue(), radiusMeters));
    }

    /**
     * 지도에서 지정한 사각형 범위 안의 건물만 받아온다.
     *
     * 반경은 원이라 시장 골목처럼 한쪽으로 긴 모양에서는 필요 없는 사방이 함께 들어온다.
     * 필요한 영역만 집어서 받으면 나중에 정리할 일이 줄어든다.
     */
    public FetchResult fetchInBounds(
            double minLongitude, double minLatitude, double maxLongitude, double maxLatitude) {
        return fetch(String.format(Locale.ROOT, "BOX(%.8f,%.8f,%.8f,%.8f)",
                minLongitude, minLatitude, maxLongitude, maxLatitude));
    }

    private FetchResult fetch(String boxFilter) {
        if (!isConfigured()) {
            throw new BuildingImportException(
                    "브이월드 인증키가 설정되지 않았습니다. 서버 환경변수 VWORLD_API_KEY를 확인해 주세요.");
        }

        List<FetchedBuilding> buildings = new ArrayList<>();
        int featureCount = 0;
        int page = 1;
        int pageTotal = 1;

        while (page <= pageTotal && page <= MAX_PAGES) {
            JsonNode response = requestPage(boxFilter, page);

            // 결과가 없으면 브이월드는 status를 NOT_FOUND로 준다. 오류가 아니라 빈 결과다.
            String status = response.path("status").asText("");
            if ("NOT_FOUND".equalsIgnoreCase(status)) {
                break;
            }

            pageTotal = Math.max(1, response.path("page").path("total").asInt(1));

            JsonNode features = response.path("result").path("featureCollection").path("features");
            for (JsonNode feature : features) {
                featureCount++;
                buildings.addAll(toBuildings(feature));
            }
            page++;
        }

        if (pageTotal > MAX_PAGES) {
            log.warn("브이월드 건물 조회 페이지가 상한({})을 넘었습니다(total={}). 앞부분만 적재합니다.",
                    MAX_PAGES, pageTotal);
        }

        return new FetchResult(buildings, featureCount, page - 1);
    }

    private JsonNode requestPage(String boxFilter, int page) {
        String uri = UriComponentsBuilder.fromPath("/req/data")
                .queryParam("service", "data")
                .queryParam("request", "GetFeature")
                .queryParam("data", buildingLayer)
                .queryParam("key", apiKey)
                .queryParam("domain", domain)
                .queryParam("format", "json")
                .queryParam("crs", "EPSG:4326")
                .queryParam("size", PAGE_SIZE)
                .queryParam("page", page)
                .queryParam("geomFilter", boxFilter)
                .build()
                .toUriString();

        String body;
        try {
            body = vworldWebClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();
        } catch (Exception e) {
            throw new BuildingImportException("브이월드 호출에 실패했습니다: " + e.getMessage(), e);
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (Exception e) {
            throw new BuildingImportException("브이월드 응답을 해석하지 못했습니다.", e);
        }

        JsonNode response = root.path("response");
        String status = response.path("status").asText("");
        if (!"OK".equalsIgnoreCase(status) && !"NOT_FOUND".equalsIgnoreCase(status)) {
            // 인증키·도메인이 틀리면 여기로 온다. 원문 메시지를 그대로 보여줘야 원인을 안다.
            String message = response.path("error").path("text").asText("알 수 없는 오류");
            throw new BuildingImportException("브이월드가 요청을 거부했습니다: " + message);
        }
        return response;
    }

    /**
     * feature 하나를 저장 가능한 건물 목록으로 바꾼다.
     * MultiPolygon이면 조각 수만큼 나오고, 속성은 조각들이 그대로 나눠 갖는다.
     *
     * package-private인 이유: 브이월드 실제 응답으로 이 변환만 따로 검증하기 위해서다
     * (VworldBuildingClientTest). 좌표가 한 겹 어긋나도 예외가 안 나고 엉뚱한 도형이
     * 저장되는 종류라 자동 검증이 필요하다.
     */
    List<FetchedBuilding> toBuildings(JsonNode feature) {
        JsonNode properties = feature.path("properties");

        String managementNumber = properties.path("bd_mgt_sn").asText("").trim();
        if (managementNumber.length() < MIN_MANAGEMENT_NUMBER_LENGTH) {
            log.debug("건물관리번호가 짧아 건너뜁니다: '{}'", managementNumber);
            return List.of();
        }
        int floors = parseFloors(properties.path("gro_flo_co").asText(""));

        // ⚠️ 저장 키로 PNU(앞 19자리)를 쓰면 안 된다. PNU는 필지 번호라 한 필지에 건물이
        // 여러 동이면 값이 겹치고, mrkbldg01m.pnu_code에는 UNIQUE 제약이 걸려 있어
        // 두 번째 건물부터 적재가 통째로 실패한다(해운대전통시장에서 확인).
        // 건물관리번호(25자리)는 건물마다 유일하므로 그대로 쓴다.
        List<String> polygons = toPolygonJsons(feature.path("geometry"));
        List<FetchedBuilding> result = new ArrayList<>();
        for (int index = 0; index < polygons.size(); index++) {
            // 한 건물이 여러 조각(MultiPolygon)으로 오면 조각마다 한 행이 되므로
            // 번호를 붙여 구분한다. 조각이 하나뿐이면 건물관리번호를 그대로 둔다.
            String pnuCode = polygons.size() == 1
                    ? managementNumber
                    : managementNumber + "-" + (index + 1);
            result.add(new FetchedBuilding(pnuCode, polygons.get(index), floors));
        }
        return result;
    }

    /**
     * geometry를 GeoJSON Polygon 문자열 목록으로 푼다.
     *
     * MultiPolygon을 그대로 저장하지 않는 이유: SIM parse_polygon이
     * "Polygon이 아닌 지오메트리"로 예외를 던지고, FE geoJsonToVertices는 한 겹 더
     * 깊은 배열을 좌표로 오해해 조용히 엉뚱한 도형을 그린다. 기존 데이터
     * (mrkaddr01d·mrkcctv01m)도 전부 Polygon이라 형식을 하나로 맞춘다.
     *
     * 구멍(hole)은 버리고 바깥 링만 쓴다. 건물 안쪽 중정까지 통행 가능 영역으로
     * 살려낼 필요가 없고, 살리면 도형이 복잡해져 격자 계산만 무거워진다.
     */
    private List<String> toPolygonJsons(JsonNode geometry) {
        String type = geometry.path("type").asText("");
        JsonNode coordinates = geometry.path("coordinates");
        List<String> result = new ArrayList<>();

        if ("Polygon".equalsIgnoreCase(type)) {
            if (coordinates.isArray() && !coordinates.isEmpty()) {
                result.add(toPolygonJson(coordinates.get(0)));
            }
        } else if ("MultiPolygon".equalsIgnoreCase(type)) {
            for (JsonNode polygon : coordinates) {
                if (polygon.isArray() && !polygon.isEmpty()) {
                    result.add(toPolygonJson(polygon.get(0)));
                }
            }
        } else {
            log.debug("다룰 수 없는 지오메트리라 건너뜁니다: {}", type);
        }
        return result;
    }

    private String toPolygonJson(JsonNode outerRing) {
        ObjectNode geometry = objectMapper.createObjectNode();
        geometry.put("type", "Polygon");
        ArrayNode rings = geometry.putArray("coordinates");
        rings.add(outerRing.deepCopy());
        return geometry.toString();
    }

    /** 층수가 비었거나 숫자가 아니면 1층으로 본다(높이를 0으로 두면 건물이 사라진다). */
    private int parseFloors(String raw) {
        try {
            return Math.max(1, Integer.parseInt(raw.trim()));
        } catch (Exception e) {
            return 1;
        }
    }

    /**
     * 중심 좌표와 반경(m)을 브이월드 geomFilter의 BOX 문자열로 바꾼다.
     * 형식은 BOX(최소경도,최소위도,최대경도,최대위도)다.
     */
    private String toBoxFilter(double latitude, double longitude, int radiusMeters) {
        double deltaLat = radiusMeters / METERS_PER_DEGREE_LAT;
        double deltaLon = radiusMeters / (METERS_PER_DEGREE_LAT * Math.cos(Math.toRadians(latitude)));

        return String.format(Locale.ROOT, "BOX(%.8f,%.8f,%.8f,%.8f)",
                longitude - deltaLon, latitude - deltaLat,
                longitude + deltaLon, latitude + deltaLat);
    }
}
