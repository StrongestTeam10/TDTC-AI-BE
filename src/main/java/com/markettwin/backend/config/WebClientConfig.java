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
