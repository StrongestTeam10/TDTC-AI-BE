package com.markettwin.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * COMCODE01M - 공통코드
 *
 * code_cob(공통코드분류) 컬럼이 추가되면서 PK가 code 단독에서
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

    /**
     * 표시 이름만 바꾼다(시장 이름 수정 시 MKT 도메인 코드명 동기화).
     *
     * 새 객체를 만들어 save()하지 않고 이 메서드로 고치는 이유: 이 엔티티는 복합키
     * (@IdClass)라 Spring Data가 "새 엔티티"로 오판해 merge 대신 persist를 시도할 수
     * 있고, 그러면 PK 중복으로 실패한다. 트랜잭션 안에서 조회한 엔티티를 직접 고치면
     * 더티 체킹이 UPDATE를 내보낸다(CctvZone.updateDetails와 같은 방식).
     */
    public void rename(String codeName) {
        this.codeName = codeName;
    }
}
