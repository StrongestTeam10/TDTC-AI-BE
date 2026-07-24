package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.CommonCode;
import com.markettwin.backend.domain.entity.CommonCodeId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 2026-07-24 변경: PK가 (code_cob, code) 복합키로 바뀌면서 ID 타입을
 * String -> CommonCodeId로 변경. 도메인(code_cob) 기준 조회/존재 확인 메서드 추가
 * (기존에는 CommonCodeService가 code.startsWith(domain) 문자열 매칭으로 대체하던 것을
 * 실제 컬럼 기반 조회로 교체).
 */
public interface CommonCodeRepository extends JpaRepository<CommonCode, CommonCodeId> {

    List<CommonCode> findByCodeCob(String codeCob);

    boolean existsByCodeCobAndCode(String codeCob, String code);
}
