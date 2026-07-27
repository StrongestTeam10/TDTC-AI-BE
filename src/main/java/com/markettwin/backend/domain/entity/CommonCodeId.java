package com.markettwin.backend.domain.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 2026-07-24 추가
 * COMCODE01M의 PK가 code 단독에서 (code_cob, code) 복합키로 바뀌면서 필요해진
 * JPA 복합키 클래스. 필드명이 CommonCode 엔티티의 @Id 필드명과 정확히 일치해야 함
 * (@IdClass 규약).
 */
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CommonCodeId implements Serializable {
    private String codeCob;
    private String code;
}
