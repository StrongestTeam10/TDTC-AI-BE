package com.markettwin.backend.dto.response;

import lombok.Builder;
import lombok.Getter;

// 2026-07-24 추가
// FE constants/orgCode.ts에 하드코딩된 ORG 옵션을 대체하기 위한 공통코드 조회용 DTO
@Getter
@Builder
public class CommonCodeDto {
    private String code;
    private String codeName;
}
