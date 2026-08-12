package com.markettwin.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.markettwin.backend.domain.entity.MarketObjectConfig;
import com.markettwin.backend.domain.entity.User;
import com.markettwin.backend.dto.request.CorridorPolicyDto;
import com.markettwin.backend.dto.request.MarketObjectConfigSaveRequestDto;
import com.markettwin.backend.dto.request.PlacedObjectDto;
import com.markettwin.backend.dto.response.MarketObjectConfigDto;
import com.markettwin.backend.exception.ForbiddenActionException;
import com.markettwin.backend.repository.MarketObjectConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * 2026-08-11 추가 (시장 오브젝트/구조 설정).
 *
 * 오브젝트 배치(PlacedObject[])와 통로 제어 정책(CorridorPolicy[])을 시장당 1세트로
 * 보관한다. 형식이 시뮬레이션 요청과 1:1이라 리스트 ↔ JSON 문자열만 변환한다.
 *
 * 권한은 시설(FacilityService)과 동일 - 시장 구조 등록 화면에서 함께 쓰는 기능이라
 * 관리자(ROL01) 또는 상인회(ORGMA)만.
 */
@Service
@RequiredArgsConstructor
public class MarketObjectConfigService {

    private static final String ADMIN_ROLE_CODE = "ROL01";
    // comcode01m ORG 도메인 - 시장상인회
    private static final String MERCHANT_ASSOCIATION_ORG_CODE = "ORGMA";

    private static final TypeReference<List<PlacedObjectDto>> OBJECT_LIST_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<CorridorPolicyDto>> POLICY_LIST_TYPE = new TypeReference<>() {};

    private final MarketObjectConfigRepository repository;
    private final MarketService marketService;
    private final ObjectMapper objectMapper;

    public MarketObjectConfigDto get(Long marketId, User currentUser) {
        assertCanManage(currentUser);
        marketService.getAccessibleMarket(marketId, currentUser);

        return repository.findByMarketId(marketId)
                .map(this::toDto)
                // 아직 저장된 게 없으면 빈 세트를 준다(화면이 빈 배치로 시작할 수 있게).
                .orElseGet(() -> new MarketObjectConfigDto(marketId, List.of(), List.of(), null));
    }

    @Transactional
    public MarketObjectConfigDto save(MarketObjectConfigSaveRequestDto request, User currentUser) {
        assertCanManage(currentUser);
        marketService.getAccessibleMarket(request.getMarketId(), currentUser);

        List<PlacedObjectDto> objects = request.getObjects() != null ? request.getObjects() : List.of();
        List<CorridorPolicyDto> policies =
                request.getCorridorPolicies() != null ? request.getCorridorPolicies() : List.of();

        String objectsJson = writeJson(objects);
        String policiesJson = writeJson(policies);

        MarketObjectConfig entity = repository.findByMarketId(request.getMarketId()).orElse(null);
        if (entity == null) {
            entity = MarketObjectConfig.builder()
                    .marketId(request.getMarketId())
                    .objectsJson(objectsJson)
                    .corridorPoliciesJson(policiesJson)
                    .updatedAt(Instant.now())
                    .build();
            return toDto(repository.save(entity));
        }

        entity.updateConfig(objectsJson, policiesJson);
        return toDto(entity);
    }

    private void assertCanManage(User currentUser) {
        if (currentUser == null) {
            throw new ForbiddenActionException("로그인이 필요합니다.");
        }
        boolean isAdmin = ADMIN_ROLE_CODE.equals(currentUser.getRulesCode());
        boolean isMerchantAssociation = MERCHANT_ASSOCIATION_ORG_CODE.equals(currentUser.getOrgCode());
        if (!isAdmin && !isMerchantAssociation) {
            throw new ForbiddenActionException("시장 오브젝트를 관리할 권한이 없습니다.");
        }
    }

    private MarketObjectConfigDto toDto(MarketObjectConfig entity) {
        return new MarketObjectConfigDto(
                entity.getMarketId(),
                readJson(entity.getObjectsJson(), OBJECT_LIST_TYPE),
                readJson(entity.getCorridorPoliciesJson(), POLICY_LIST_TYPE),
                entity.getUpdatedAt());
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            // 서버 내부에서 만든 리스트를 직렬화하는 것이라 사실상 실패하지 않는다.
            throw new IllegalStateException("설정 직렬화 실패", e);
        }
    }

    private <T> List<T> readJson(String json, TypeReference<List<T>> type) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            // 저장된 JSON이 깨져 있어도 화면 전체가 죽지 않도록 빈 목록으로 돌려준다.
            return List.of();
        }
    }
}
