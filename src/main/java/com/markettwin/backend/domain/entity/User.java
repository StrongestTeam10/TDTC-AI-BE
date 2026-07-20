package com.markettwin.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * USRUSRS01M - 사용자
 */
@Entity
@Table(name = "usrusrs01m")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "login_id", nullable = false, unique = true, length = 30)
    private String loginId;

    @Column(name = "password", nullable = false, length = 64)
    private String password;

    @Column(name = "name", nullable = false, length = 30)
    private String name;

    @Column(name = "roles_code", nullable = false, length = 5)
    private String rolesCode;

    @Column(name = "org_code", nullable = false, length = 5)
    private String orgCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_ip", length = 16)
    private String createdIp;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "updated_ip", length = 16)
    private String updatedIp;
}