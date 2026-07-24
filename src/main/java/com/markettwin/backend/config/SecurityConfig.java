package com.markettwin.backend.config;

import com.markettwin.backend.security.JwtAuthenticationFilter;
import com.markettwin.backend.security.JwtTokenProvider;
import com.markettwin.backend.security.RestAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 2026-07-24 추가, 같은 날 2차 변경
 * 로그인/회원가입 API 추가에 맞춰 Spring Security를 처음 도입함.
 *
 * 1차(최초 도입 시)에는 기존 API(대시보드/시뮬레이션/시장 등)가 FE에서 아직 토큰을
 * 붙여 호출하도록 연동되지 않아서 전체를 permitAll로 열어뒀었음.
 *
 * 2차 변경: FE가 로그인 후 모든 API 호출에 JWT를 Authorization 헤더로 자동 첨부하도록
 * 연동 완료됨(FE api/client.ts 인터셉터). 이에 맞춰 /api/auth/**, 공통코드 조회,
 * Swagger/Actuator 헬스체크만 permitAll로 남기고 나머지 전체 API는 authenticated()로
 * 좁힘. 팀원 담당 파이프라인 B(시나리오 시뮬레이션) 쪽도 이제 토큰 없이 호출하면
 * 401이 나므로, 팀원에게 이 변경 사항 공유 필요.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${cors.allowed-origins:http://localhost:5173}")
    private String[] allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtTokenProvider jwtTokenProvider,
            RestAuthenticationEntryPoint restAuthenticationEntryPoint
    ) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // JWT 기반 stateless API라 CSRF 토큰 불필요
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(restAuthenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/common-codes/**").permitAll() // 회원가입 화면(로그인 전)에서 소속기관 조회
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
