package com.markettwin.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.markettwin.backend.domain.entity.Facility;
import com.markettwin.backend.domain.entity.Zone;
import com.markettwin.backend.domain.entity.ZoneAdjacency;
import com.markettwin.backend.dto.response.SpatialNodeDto;
import com.markettwin.backend.repository.FacilityRepository;
import com.markettwin.backend.repository.ZoneAdjacencyRepository;
import com.markettwin.backend.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 프론트 SpatialNode(점포/통로/입구 + 좌표 + 연결 노드) 레이아웃을 구성하는 서비스.
 *
 * 현재 DB는 "구역(Zone) 단위" 그래프만 갖고 있고(mrkadjc01m는 zone-to-zone),
 * 개별 점포(Facility) 간 연결 데이터는 없다. 그래서 임시로:
 *   - Zone       -> nodeType "corridor" (여러 점포를 포함하는 구역으로 취급)
 *   - Facility(GATE) -> nodeType "entrance"
 *   - 그 외 Facility -> nodeType "stall"
 * 로 매핑하고, 게이트는 인접 테이블에 없으므로 가장 가까운 Zone과 자동 연결한다.
 * ID 충돌 방지를 위해 Facility 노드 ID는 zoneId 범위와 겹치지 않도록 +100_000 오프셋을 더한다.
 *
 * TODO: 점포 단위 정밀 그래프가 필요해지면 mrkadjc01m을 zone 단위 대신
 * facility 단위 인접 테이블로 확장하고 이 서비스도 다시 짜야 한다.
 */
@Service
@RequiredArgsConstructor
public class SpatialLayoutService {

    private static final long FACILITY_NODE_ID_OFFSET = 100_000L;

    private final ZoneRepository zoneRepository;
    private final FacilityRepository facilityRepository;
    private final ZoneAdjacencyRepository zoneAdjacencyRepository;
    private final ObjectMapper objectMapper;

    public List<SpatialNodeDto> getLayout(Long marketId) {
        List<Zone> zones = zoneRepository.findByMarketId(marketId);
        List<Facility> facilities = facilityRepository.findByMarketId(marketId);
        List<ZoneAdjacency> adjacencies =
                zoneAdjacencyRepository.findByMarketIdAndIsActiveTrue(marketId);

        Map<Long, double[]> zoneCentroids = new HashMap<>();
        for (Zone zone : zones) {
            zoneCentroids.put(zone.getZoneId(), centroidOf(zone.getPolygonCoordinates()));
        }

        // zone_id -> 연결된 노드 id 목록 (zone-zone 인접 + 근접 게이트)
        Map<Long, Set<Long>> zoneEdges = new HashMap<>();
        for (ZoneAdjacency adj : adjacencies) {
            zoneEdges.computeIfAbsent(adj.getFromZoneId(), k -> new LinkedHashSet<>())
                    .add(adj.getToZoneId());
        }

        Map<Long, Long> facilityToNearestZone = new HashMap<>();
        for (Facility facility : facilities) {
            if (facility.getLatitude() == null || facility.getLongitude() == null) continue;
            Long nearestZoneId = findNearestZone(facility, zoneCentroids);
            if (nearestZoneId != null) {
                facilityToNearestZone.put(facility.getFacilityId(), nearestZoneId);
                zoneEdges.computeIfAbsent(nearestZoneId, k -> new LinkedHashSet<>())
                        .add(facility.getFacilityId() + FACILITY_NODE_ID_OFFSET);
            }
        }

        List<SpatialNodeDto> nodes = new ArrayList<>();

        for (Zone zone : zones) {
            double[] centroid = zoneCentroids.get(zone.getZoneId());
            Set<Long> connected = zoneEdges.getOrDefault(zone.getZoneId(), Set.of());
            nodes.add(new SpatialNodeDto(
                    zone.getZoneId(),
                    "corridor",
                    centroid[0],
                    centroid[1],
                    new ArrayList<>(connected)
            ));
        }

        for (Facility facility : facilities) {
            if (facility.getLatitude() == null || facility.getLongitude() == null) continue;
            long nodeId = facility.getFacilityId() + FACILITY_NODE_ID_OFFSET;
            String nodeType = "GATE".equalsIgnoreCase(facility.getFacilityType())
                    ? "entrance" : "stall";
            Long nearestZoneId = facilityToNearestZone.get(facility.getFacilityId());
            List<Long> connected = nearestZoneId != null ? List.of(nearestZoneId) : List.of();
            nodes.add(new SpatialNodeDto(
                    nodeId,
                    nodeType,
                    facility.getLongitude().doubleValue(),
                    facility.getLatitude().doubleValue(),
                    connected
            ));
        }

        return nodes;
    }

    private Long findNearestZone(Facility facility, Map<Long, double[]> zoneCentroids) {
        double fLon = facility.getLongitude().doubleValue();
        double fLat = facility.getLatitude().doubleValue();
        Long nearest = null;
        double minDist = Double.MAX_VALUE;
        for (Map.Entry<Long, double[]> entry : zoneCentroids.entrySet()) {
            double dx = entry.getValue()[0] - fLon;
            double dy = entry.getValue()[1] - fLat;
            double dist = dx * dx + dy * dy;
            if (dist < minDist) {
                minDist = dist;
                nearest = entry.getKey();
            }
        }
        return nearest;
    }

    /** GeoJSON Polygon 문자열의 외곽 링(첫 ring)을 단순 평균해 중심점([lon, lat])을 구한다. */
    private double[] centroidOf(String polygonCoordinatesJson) {
        if (polygonCoordinatesJson == null || polygonCoordinatesJson.isBlank()) {
            return new double[]{0.0, 0.0};
        }
        try {
            JsonNode root = objectMapper.readTree(polygonCoordinatesJson);
            JsonNode ring = root.path("coordinates").path(0);
            double sumLon = 0, sumLat = 0;
            int count = 0;
            for (JsonNode point : ring) {
                sumLon += point.get(0).asDouble();
                sumLat += point.get(1).asDouble();
                count++;
            }
            if (count == 0) return new double[]{0.0, 0.0};
            return new double[]{sumLon / count, sumLat / count};
        } catch (Exception e) {
            return new double[]{0.0, 0.0};
        }
    }
}