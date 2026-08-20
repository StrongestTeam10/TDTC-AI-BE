package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.CommonCode;
import com.markettwin.backend.domain.entity.CommonCodeId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * PK가 (code_cob, code) 복합키로 바뀌면서 ID 타입을
 * String -> CommonCodeId로 변경. 도메인(code_cob) 기준 조회/존재 확인 메서드 추가
 * (기존에는 CommonCodeService가 code.startsWith(domain) 문자열 매칭으로 대체하던 것을
 * 실제 컬럼 기반 조회로 교체).
 */
public interface CommonCodeRepository extends JpaRepository<CommonCode, CommonCodeId> {

    List<CommonCode> findByCodeCob(String codeCob);

    boolean existsByCodeCobAndCode(String codeCob, String code);

    // (시장 등록): comcode01m.code_name은 도메인별이 아니라 테이블
    // 전체에 UNIQUE라, 새 시장 이름을 넣기 전에 도메인과 무관하게 중복을 확인해야 한다.
    boolean existsByCodeName(String codeName);
}
