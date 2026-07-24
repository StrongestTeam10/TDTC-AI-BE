package com.markettwin.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * COMCODE01M - 공통코드
 *
 * 2026-07-24 변경: code_cob(공통코드분류) 컬럼이 추가되면서 PK가 code 단독에서
 * (code_cob, code) 복합키로 바뀜. 복합키는 CommonCodeId(@IdClass)로 표현함.
 */
@Entity
@Table(name = "comcode01m")
@IdClass(CommonCodeId.class)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommonCode {

    @Id
    @Column(name = "code_cob", length = 3)
    private String codeCob;

    @Id
    @Column(name = "code", length = 5)
    private String code;

    @Column(name = "code_name", nullable = false, unique = true, length = 50)
    private String codeName;

    @Column(name = "describe", length = 200)
    private String describe;

    @Column(name = "rmk", length = 500)
    private String remark;
}
