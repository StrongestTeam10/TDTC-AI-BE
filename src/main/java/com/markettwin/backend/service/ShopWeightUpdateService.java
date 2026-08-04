package com.markettwin.backend.service;

import com.markettwin.backend.domain.entity.Facility;
import com.markettwin.backend.dto.NaverBlogSearchResponse;
import com.markettwin.backend.repository.FacilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopWeightUpdateService {

    private final FacilityRepository facilityRepository;
    private final WebClient naverWebClient; // from NaverApiConfig

    public void updateAllStallWeights() {
        log.info("Starting shop weight update pipeline...");
        long startTime = System.currentTimeMillis();

        // 1. 조회 단계 (트랜잭션 없이)
        List<Facility> stalls = facilityRepository.findByFacilityType("STALL");
        if (stalls.isEmpty()) {
            log.info("No stalls found. Skipping update.");
            return;
        }

        Map<Facility, Integer> buzzCounts = new HashMap<>();

        // 2. API 호출 단계 (Fault-Tolerant)
        for (Facility stall : stalls) {
            String shopName = stall.getName();
            String query = "망원시장 " + shopName;

            try {
                NaverBlogSearchResponse response = naverWebClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/v1/search/blog.json")
                                .queryParam("query", query)
                                .queryParam("display", 1) // 우리는 total만 필요하므로 1개만 요청
                                .build())
                        .retrieve()
                        .bodyToMono(NaverBlogSearchResponse.class)
                        .block(); // 동기 호출 (스케줄러 환경이므로 허용됨)

                if (response != null && response.total() != null) {
                    buzzCounts.put(stall, response.total());
                }

                log.info("Naver API Response for shop '{}': {}", shopName, response);
            } catch (WebClientResponseException e) {
                log.warn("Failed to fetch Naver API for shop: {}, status: {}", shopName, e.getStatusCode());
                // 에러 발생 상점은 buzzCounts 에 넣지 않고 Skip (기존 weight 유지)
            } catch (Exception e) {
                log.error("Unexpected error while fetching Naver API for shop: {}", shopName, e);
                // 에러 발생 상점은 buzzCounts 에 넣지 않고 Skip
            }

            // Rate Limit 회피
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Thread interrupted during sleep", e);
                break; // 인터럽트 발생 시 전체 루프 중단
            }
        }

        if (buzzCounts.isEmpty()) {
            log.warn("No successful API calls. Aborting update.");
            return;
        }

        // 3. 알고리즘 계산 단계
        Map<Facility, Double> logMap = new HashMap<>();
        double maxLog = Double.MIN_VALUE;
        double minLog = Double.MAX_VALUE;

        for (Map.Entry<Facility, Integer> entry : buzzCounts.entrySet()) {
            Facility stall = entry.getKey();
            Integer buzzCount = entry.getValue();

            double logVal = Math.log10(buzzCount + 1);
            logMap.put(stall, logVal);

            if (logVal > maxLog)
                maxLog = logVal;
            if (logVal < minLog)
                minLog = logVal;
        }

        List<Facility> updatedStalls = new ArrayList<>();

        for (Map.Entry<Facility, Double> entry : logMap.entrySet()) {
            Facility stall = entry.getKey();
            double logVal = entry.getValue();
            double weight;

            if (maxLog == minLog) {
                weight = 0.5;
            } else {
                weight = (logVal - minLog) / (maxLog - minLog);
                // 소수점 3자리 반올림
                weight = Math.round(weight * 1000.0) / 1000.0;
            }

            stall.updateWeight(weight);
            updatedStalls.add(stall);
            log.info("Calculated weight for stall '{}': {}", stall.getName(), weight);
        }

        // 4. 저장 단계 (트랜잭션 분리)
        saveAll(updatedStalls);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Finished updating weights for {} stalls in {} ms.", updatedStalls.size(), duration);
    }

    @Transactional
    public void saveAll(List<Facility> facilities) {
        facilityRepository.saveAll(facilities);
    }
}
