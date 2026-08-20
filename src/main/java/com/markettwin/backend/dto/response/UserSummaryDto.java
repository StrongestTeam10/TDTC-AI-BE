package com.markettwin.backend.dto.response;

import lombok.Builder;
import lombok.Getter;

// FE authStore.ts의 AuthUser 타입과 필드명을 맞춰둠 (loginId/name/rulesCode/orgCode)
@Getter
@Builder
public class UserSummaryDto {
    private Long userId;
    private String loginId;
    private String name;
    private String rulesCode;
    private String orgCode;
    // 추가(게시판): FE가 "내 시장" 표시나 글쓰기 화면 안내에 참고할 수 있도록 포함
    private String marketCode;
    // 어드민 화면에서 볼 수 있도록 전화번호, 당직 여부 포함
    private String phoneNumber;
    private Boolean isDuty;
}
