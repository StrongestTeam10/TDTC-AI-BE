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
@Table(name = "comcode01m")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommonCode {

    @Id
    @Column(name = "code", length = 3)
    private String code;

    @Column(name = "code_name", nullable = false, unique = true, length = 50)
    private String codeName;

    @Column(name = "describe", length = 200)
    private String describe;

    @Column(name = "mrk", length = 500)
    private String remark;
}
