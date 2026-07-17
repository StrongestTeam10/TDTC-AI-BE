package com.markettwin.backend.dto.response;

import java.util.List;

/**
 * 프론트엔드 src/types/index.ts 의 SpatialNode 와 필드명이 반드시 일치해야 함.
 */
public record SpatialNodeDto(
        Long nodeId,
        String nodeType,       // "stall" | "corridor" | "entrance"
        Double xCoord,
        Double yCoord,
        List<Long> connectedNodes
) {
}
