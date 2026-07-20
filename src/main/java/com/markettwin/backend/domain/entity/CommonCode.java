package com.markettwin.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * COMCODE01M - 공통코드
 */
@Entity
@Table(name = "COMCODE01M")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommonCode {

    @Id
    @Column(name = "code", length = 3)
    private String code;

    @Column(name = "code_name", nullable = false, unique = true, length = 30)
    private String codeName;

    @Column(name = "code_desc", length = 200)
    private String description;

    @Column(name = "mrk", length = 200)
    private String remark;
}
