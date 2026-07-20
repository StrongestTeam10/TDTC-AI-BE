package com.markettwin.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * ENTCHAN01H - 협장변경 (승인/변경 이력)
 */
@Entity
@Table(name = "ENTCHAN01H")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntityChangeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "change_id")
    private Long changeId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "change_type", length = 10)
    private String changeType;

    @Lob
    @Column(name = "before_data")
    private String beforeData;

    @Lob
    @Column(name = "after_data")
    private String afterData;

    @Column(name = "status", length = 15)
    private String status;

    @Column(name = "requested_at")
    private Instant requestedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;
}
