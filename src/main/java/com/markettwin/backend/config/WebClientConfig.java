package com.markettwin.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${simulation-engine.base-url}")
    private String simulationEngineBaseUrl;

    // WebClient 기본 응답 버퍼 한도(256KB)는 예측/시나리오 스텝 수가 늘어나면
    // 프레임(frames) 응답이 쉽게 넘어선다. 2026-07-24: predict steps=30 정도에서도
    // DataBufferLimitException -> SimulationEngineException -> 502로 이어지는 걸
    // 확인해서 10MB로 상향함.
    private static final int MAX_IN_MEMORY_SIZE = 10 * 1024 * 1024;

    @Value("${vworld.base-url}")
    private String vworldBaseUrl;

    /**
     * 2026-08-14 추가: 브이월드(공간정보 오픈플랫폼) 호출용.
     *
     * 시뮬레이션 엔진과 버퍼 한도를 같이 쓰는 이유: 건물 폴리곤은 한 페이지에 최대
     * 1000건이고 각 건물이 꼭짓점 수십 개를 가져서, 기본값 256KB로는 넘어선다.
     */
    @Bean
    public WebClient vworldWebClient() {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs()
                        .maxInMemorySize(MAX_IN_MEMORY_SIZE))
                .build();

        return WebClient.builder()
                .baseUrl(vworldBaseUrl)
                .exchangeStrategies(strategies)
                .build();
    }

    /**
     * 2026-08-14 추가: OpenStreetMap(Overpass) 호출용.
     *
     * baseUrl을 두지 않는 이유: Overpass는 공개 미러가 여럿이라 엔드포인트를 통째로
     * 설정값(osm.overpass-url)으로 받는다. User-Agent를 명시하는 것은 Overpass 운영
     * 정책이 익명 클라이언트를 차단할 수 있기 때문이다.
     */
    @Bean
    public WebClient osmWebClient() {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs()
                        .maxInMemorySize(MAX_IN_MEMORY_SIZE))
                .build();

        return WebClient.builder()
                .defaultHeader("User-Agent", "TDTC-market-twin/1.0 (KT AIVLE School project)")
                .exchangeStrategies(strategies)
                .build();
    }

    @Bean
    public WebClient simulationEngineWebClient() {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs()
                        .maxInMemorySize(MAX_IN_MEMORY_SIZE))
                .build();

        return WebClient.builder()
                .baseUrl(simulationEngineBaseUrl)
                .exchangeStrategies(strategies)
                .build();
    }
}
