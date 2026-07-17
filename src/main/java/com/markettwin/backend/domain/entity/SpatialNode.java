package com.markettwin.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "spatial_nodes")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpatialNode {

    @Id
    @Column(name = "node_id")
    private Long nodeId;

    @Column(name = "node_type", nullable = false, length = 20)
    private String nodeType;   // 'stall', 'corridor', 'entrance'

    @Column(name = "x_coord", nullable = false)
    private Double xCoord;

    @Column(name = "y_coord", nullable = false)
    private Double yCoord;

    @Column(name = "width")
    private Double width;

    @ElementCollection
    @CollectionTable(name = "spatial_node_edges", joinColumns = @JoinColumn(name = "node_id"))
    @Column(name = "connected_node_id")
    private List<Long> connectedNodes;
}
