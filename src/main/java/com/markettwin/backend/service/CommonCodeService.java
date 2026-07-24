package com.markettwin.backend.service;

import com.markettwin.backend.dto.response.CommonCodeDto;
import com.markettwin.backend.repository.CommonCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 2026-07-24 추가, 같은 날 2차 변경
 * FE constants/orgCode.ts에 하드코딩돼 있던 소속기관(ORG) 옵션을 실제 DB(comcode01m)
 * 조회로 대체하기 위한 서비스.
 *
 * 2차 변경: comcode01m에 code_cob(공통코드분류) 컬럼이 생기면서, 처음에 임시로 썼던
 * "code.startsWith(domain)" 문자열 매칭을 실제 code_cob 컬럼 조회로 교체함.
 */
@Service
@RequiredArgsConstructor
public class CommonCodeService {

    private final CommonCodeRepository commonCodeRepository;

    public List<CommonCodeDto> getCodesByDomain(String codeCob) {
        return commonCodeRepository.findByCodeCob(codeCob).stream()
                .map(c -> CommonCodeDto.builder()
                        .code(c.getCode())
                        .codeName(c.getCodeName())
                        .build())
                .toList();
    }
}
