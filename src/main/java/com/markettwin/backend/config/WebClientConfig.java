package com.markettwin.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${simulation-engine.base-url}")
    private String simulationEngineBaseUrl;

    @Bean
    public WebClient simulationEngineWebClient() {
        return WebClient.builder()
                .baseUrl(simulationEngineBaseUrl)
                .build();
    }
}
