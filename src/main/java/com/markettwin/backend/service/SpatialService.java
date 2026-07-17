package com.markettwin.backend.service;

import com.markettwin.backend.domain.entity.SpatialNode;
import com.markettwin.backend.dto.response.SpatialNodeDto;
import com.markettwin.backend.repository.SpatialNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SpatialService {

    private final SpatialNodeRepository spatialNodeRepository;

    public List<SpatialNodeDto> getLayout() {
        return spatialNodeRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    private SpatialNodeDto toDto(SpatialNode node) {
        return new SpatialNodeDto(
                node.getNodeId(),
                node.getNodeType(),
                node.getXCoord(),
                node.getYCoord(),
                node.getConnectedNodes()
        );
    }
}
