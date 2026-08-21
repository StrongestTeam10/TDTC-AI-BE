package com.markettwin.backend.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 브이월드 건물 응답을 우리 저장 형식으로 바꾸는 변환을 확인한다.
 *
 * 이 변환이 틀리면 예외가 안 나고 "그럴듯하지만 엉뚱한 위치의 도형"이 DB에 들어간다.
 * 특히 브이월드는 geometry를 MultiPolygon으로 주는데, 그대로 저장하면
 * SIM parse_polygon이 거부하고 FE geoJsonToVertices는 한 겹 더 깊은 배열을 좌표로
 * 오해해 조용히 깨진다. 아래 JSON은 에 실제 응답에서 가져온 값이다.
 */
class VworldBuildingClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final VworldBuildingClient client = new VworldBuildingClient(null, objectMapper);

    /** 망원로 84-5, 지상 5층. 브이월드 실제 응답. */
    private static final String REAL_FEATURE = """
            {
              "properties": {
                "buld_nm_dc": "", "buld_no": "84-5", "rd_nm": "망원로",
                "sigungu": "마포구", "sido": "서울특별시", "gro_flo_co": "5",
                "bul_eng_nm": "", "buld_nm": "", "gu": "망원동",
                "bd_mgt_sn": "1144012300104860028000001"
              },
              "geometry": {
                "type": "MultiPolygon",
                "coordinates": [[[
                  [126.90623235922311, 37.557388515520046],
                  [126.906363526723, 37.55741734484416],
                  [126.90639681349414, 37.557317911422786],
                  [126.90634742711282, 37.55730642905971],
                  [126.90634380994878, 37.557320733859136],
                  [126.90625939094221, 37.557304213714545],
                  [126.90623235922311, 37.557388515520046]
                ]]]
              }
            }
            """;

    @Test
    @DisplayName("실제 응답을 Polygon 한 건으로 바꾸고 좌표를 그대로 보존한다")
    void convertsRealMultiPolygonFeature() throws Exception {
        List<VworldBuildingClient.FetchedBuilding> buildings = client.toBuildings(read(REAL_FEATURE));

        assertThat(buildings).hasSize(1);
        VworldBuildingClient.FetchedBuilding building = buildings.get(0);

        JsonNode saved = objectMapper.readTree(building.polygonCoordinates());
        assertThat(saved.path("type").asText()).isEqualTo("Polygon");

        JsonNode ring = saved.path("coordinates").get(0);
        assertThat(ring).hasSize(7);
        // GeoJSON은 [경도, 위도] 순. 뒤집히면 지도에서 엉뚱한 곳을 가리킨다.
        assertThat(ring.get(0).get(0).asDouble()).isEqualTo(126.90623235922311);
        assertThat(ring.get(0).get(1).asDouble()).isEqualTo(37.557388515520046);
        // 링은 첫 점과 끝 점이 같아야 한다.
        assertThat(ring.get(6)).isEqualTo(ring.get(0));
    }

    @Test
    @DisplayName("건물관리번호를 통째로 저장 키로 쓴다 - 앞 19자리(PNU)만 쓰면 안 된다")
    void keepsWholeManagementNumberAsKey() throws Exception {
        List<VworldBuildingClient.FetchedBuilding> buildings = client.toBuildings(read(REAL_FEATURE));

        // PNU(앞 19자리)는 필지 번호라 한 필지에 건물이 여러 동이면 겹친다.
        // mrkbldg01m.pnu_code에는 UNIQUE 제약이 있어서, PNU를 쓰면 두 번째 건물부터
        // 적재가 통째로 실패한다(해운대전통시장에서 실제로 터졌다).
        assertThat(buildings.get(0).pnuCode()).isEqualTo("1144012300104860028000001");
        assertThat(buildings.get(0).pnuCode()).startsWith("1144012300104860028");
    }

    @Test
    @DisplayName("지상층수를 그대로 읽는다")
    void readsGroundFloorCount() throws Exception {
        List<VworldBuildingClient.FetchedBuilding> buildings = client.toBuildings(read(REAL_FEATURE));

        assertThat(buildings.get(0).floors()).isEqualTo(5);
    }

    @Test
    @DisplayName("조각이 여럿인 건물은 조각 수만큼 나뉘고 속성은 공유한다")
    void splitsMultiPolygonIntoSeparateBuildings() throws Exception {
        String twoParts = """
                {
                  "properties": { "gro_flo_co": "3", "bd_mgt_sn": "1144012300104860028000002" },
                  "geometry": {
                    "type": "MultiPolygon",
                    "coordinates": [
                      [[[126.9, 37.5], [126.91, 37.5], [126.91, 37.51], [126.9, 37.5]]],
                      [[[126.92, 37.52], [126.93, 37.52], [126.93, 37.53], [126.92, 37.52]]]
                    ]
                  }
                }
                """;

        List<VworldBuildingClient.FetchedBuilding> buildings = client.toBuildings(read(twoParts));

        assertThat(buildings).hasSize(2);
        // 조각마다 한 행이 되므로 저장 키가 겹치면 안 된다. 번호를 붙여 구분한다.
        assertThat(buildings).extracting(VworldBuildingClient.FetchedBuilding::pnuCode)
                .containsExactly("1144012300104860028000002-1", "1144012300104860028000002-2");
        assertThat(buildings).allSatisfy(building -> assertThat(building.floors()).isEqualTo(3));
    }

    @Test
    @DisplayName("Polygon으로 오는 경우도 그대로 받는다")
    void acceptsPlainPolygon() throws Exception {
        String plain = """
                {
                  "properties": { "gro_flo_co": "2", "bd_mgt_sn": "1144012300104860028000003" },
                  "geometry": {
                    "type": "Polygon",
                    "coordinates": [[[126.9, 37.5], [126.91, 37.5], [126.91, 37.51], [126.9, 37.5]]]
                  }
                }
                """;

        List<VworldBuildingClient.FetchedBuilding> buildings = client.toBuildings(read(plain));

        assertThat(buildings).hasSize(1);
        JsonNode saved = objectMapper.readTree(buildings.get(0).polygonCoordinates());
        assertThat(saved.path("type").asText()).isEqualTo("Polygon");
        assertThat(saved.path("coordinates").get(0)).hasSize(4);
    }

    @Test
    @DisplayName("층수가 비어 있으면 1층으로 본다")
    void defaultsMissingFloorCountToOne() throws Exception {
        String noFloors = """
                {
                  "properties": { "gro_flo_co": "", "bd_mgt_sn": "1144012300104860028000004" },
                  "geometry": {
                    "type": "Polygon",
                    "coordinates": [[[126.9, 37.5], [126.91, 37.5], [126.91, 37.51], [126.9, 37.5]]]
                  }
                }
                """;

        List<VworldBuildingClient.FetchedBuilding> buildings = client.toBuildings(read(noFloors));

        // 0층으로 두면 height_m이 0이 되어 건물이 없는 것과 같아진다.
        assertThat(buildings.get(0).floors()).isEqualTo(1);
    }

    @Test
    @DisplayName("건물관리번호가 짧으면 건너뛴다")
    void skipsFeatureWithoutUsableManagementNumber() throws Exception {
        String broken = """
                {
                  "properties": { "gro_flo_co": "5", "bd_mgt_sn": "114401" },
                  "geometry": {
                    "type": "Polygon",
                    "coordinates": [[[126.9, 37.5], [126.91, 37.5], [126.91, 37.51], [126.9, 37.5]]]
                  }
                }
                """;

        assertThat(client.toBuildings(read(broken))).isEmpty();
    }

    private JsonNode read(String json) throws Exception {
        return objectMapper.readTree(json);
    }
}
