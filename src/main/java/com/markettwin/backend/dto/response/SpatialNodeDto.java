package com.markettwin.backend.dto.response;

import java.util.List;

/**
 * 프론트 SpatialNode 타입과 필드명을 반드시 일치시킬 것
 * (nodeId/nodeType/xCoord/yCoord/connectedNodes).
 */
public record SpatialNodeDto(
        Long nodeId,
        String nodeType,   // "stall" | "corridor" | "entrance"
        Double xCoord,     // 경도(longitude)
        Double yCoord,     // 위도(latitude)
        List<Long> connectedNodes
) {
}