package com.markettwin.backend.service;

import com.markettwin.backend.domain.entity.CommonCode;
import com.markettwin.backend.repository.CommonCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 2026-07-31 추가 (보고서 기능)
 * 시나리오를 사람이 읽을 수 있는 이름으로 바꾼다.
 *
 * 시뮬레이션 저장 로직이 시나리오명을 "시나리오 2026-07-30T06:37:23.787238900Z" 처럼
 * 실행 시각만으로 만든다. 이 값이 보고서 문서의 시나리오 구성표와 결과 비교표에 그대로
 * 실려 정책 보고서로서 읽기 어렵고, 목록에서도 어떤 시나리오인지 구분되지 않는다.
 *
 * DB 값을 고치지 않고 조회·조립 시점에만 대체한다. 나중에 사용자가 시나리오 이름을 직접
 * 입력하는 기능이 생기면, 자동 생성 형태가 아니게 되어 이 대체가 자연히 비활성화된다.
 */
@Component
@RequiredArgsConstructor
public class ScenarioDisplayNameResolver {

    /** 공통코드 분류: 시나리오 정책 유형(POLNO/POLFR/POLAC/POLCB). */
    private static final String POLICY_CODE_GROUP = "POL";

    /** 정책 유형을 특정하지 못하는 코드. 이름에 넣어도 정보가 되지 않는다. */
    private static final String NEUTRAL_POLICY_CODE = "POLNO";

    /**
     * 자동 생성된 시나리오명 형태.
     * 예: "시나리오 2026-07-30T06:37:23.787238900Z"
     * 이 형태일 때만 대체하고, 사람이 붙인 이름은 그대로 존중한다.
     */
    private static final Pattern AUTO_GENERATED =
            Pattern.compile("^시나리오\\s+\\d{4}-\\d{2}-\\d{2}T.*$");

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    .withZone(ZoneId.of("Asia/Seoul"));

    private final CommonCodeRepository commonCodeRepository;

    /**
     * 표시용 시나리오명을 만든다.
     *
     * 자동 생성 형태가 아니면 원본을 그대로 돌려준다.
     * 대체할 때는 "{시장명} {정책유형} 시나리오 (yyyy-MM-dd HH:mm)" 형태로 만든다.
     * 시각은 UTC로 저장돼 있어 KST로 바꿔 표기한다.
     */
    public String resolve(
            String storedName, String marketName, String policyTypeCode, Instant regDatetime) {

        if (storedName != null && !AUTO_GENERATED.matcher(storedName.trim()).matches()) {
            return storedName;
        }

        StringBuilder name = new StringBuilder();
        if (marketName != null && !marketName.isBlank()) {
            name.append(marketName).append(' ');
        }

        String policyLabel = policyLabelOf(policyTypeCode);
        if (policyLabel != null) {
            name.append(policyLabel).append(' ');
        }
        name.append("시나리오");

        if (regDatetime != null) {
            name.append(" (").append(STAMP.format(regDatetime)).append(')');
        }
        return name.toString();
    }

    /**
     * 정책 코드의 한글 표기. 공통코드(comcode01m)를 그대로 쓰므로 코드 표가 바뀌면 함께 따라간다.
     * POLNO(없음)는 이름에 넣지 않는다 - "망원시장 없음 시나리오"가 되어 오히려 읽기 나쁘다.
     */
    private String policyLabelOf(String policyTypeCode) {
        if (policyTypeCode == null
                || policyTypeCode.isBlank()
                || NEUTRAL_POLICY_CODE.equals(policyTypeCode)) {
            return null;
        }
        Map<String, String> labels = commonCodeRepository.findByCodeCob(POLICY_CODE_GROUP)
                .stream()
                .collect(Collectors.toMap(CommonCode::getCode, CommonCode::getCodeName,
                        (first, second) -> first));
        return labels.get(policyTypeCode);
    }
}
