package com.markettwin.backend.config;

import com.markettwin.backend.security.JwtAuthenticationFilter;
import com.markettwin.backend.security.JwtTokenProvider;
import com.markettwin.backend.security.RestAccessDeniedHandler;
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

import java.util.ArrayList;
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

    /**
     * 관제 기능(실시간 대시보드·시뮬레이션·정책 보고서)을 쓸 수 있는 권한.
     * ROL01 관리자, ROL02 관제요원.
     *
     * 2026-08-06: 이름을 SIMULATION_ROLES에서 바꿨다. 대시보드까지 같은 두 권한으로
     * 제한하게 되면서 "시뮬레이션"이라는 이름이 범위를 좁게 말하게 됐다.
     *
     * hasAnyRole은 값 앞에 "ROLE_"을 붙여 비교하고, JwtAuthenticationFilter도
     * "ROLE_" + rulesCode 형태로 권한을 만들므로 코드값만 그대로 적으면 된다.
     */
    private static final String[] CONTROL_SYSTEM_ROLES = {"ROL01", "ROL02"};

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtTokenProvider jwtTokenProvider,
            RestAuthenticationEntryPoint restAuthenticationEntryPoint,
            RestAccessDeniedHandler restAccessDeniedHandler
    ) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // JWT 기반 stateless API라 CSRF 토큰 불필요
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                // 401(로그인 필요)과 403(권한 없음)을 나눠 응답한다. 거부 처리를 등록하지
                // 않으면 권한 부족도 401로 나가고, FE가 그걸 세션 만료로 보고 로그아웃시킨다.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/common-codes/**").permitAll() // 회원가입 화면(로그인 전)에서 소속기관 조회
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // 2026-08-05 추가: 시뮬레이션은 관리자(ROL01)와 관제요원(ROL02)만 쓴다.
                        // 상인회(ROL03)는 What-if 실험과 그 결과로 만든 정책 보고서를 다루지 않는다.
                        //
                        // 경로 전체를 막는 이유: 개별 엔드포인트를 나열하면 /api/simulation 아래에
                        // 새 API가 생길 때 목록에 추가하는 걸 잊어도 아무 경고 없이 열린 채로 남는다.
                        // 기본을 막고 필요한 것만 여는 편이 안전하다.
                        //
                        // 이 범위에는 관제용인 GET /api/simulation/coordinates/frame(CCTV 보행자
                        // 좌표)도 포함된다. 경로 접두사만 공유할 뿐 성격이 다른 API지만, 관제요원이
                        // ROL02라 실제로 막히는 대상은 상인회뿐이라 함께 두었다.
                        .requestMatchers("/api/simulation/**").hasAnyRole(CONTROL_SYSTEM_ROLES)
                        // 2026-08-06 추가: 실시간 관제 대시보드도 같은 두 권한으로 제한한다.
                        // 상인회(ROL03)는 실시간 관제 대상이 아니다 - 상인회에게 열려 있는 것은
                        // 게시판과 상점 위치 등록(/api/facilities/**)이다.
                        //
                        // /api/markets/** 는 여기에 넣지 않았다. 상점 위치 등록 화면이 시장
                        // 목록을 조회하므로 막으면 상인회가 쓸 수 있는 화면이 함께 죽는다.
                        // 시장별 접근 범위는 MarketService.getAccessibleMarket이 따로 좁힌다.
                        .requestMatchers("/api/dashboard/**").hasAnyRole(CONTROL_SYSTEM_ROLES)
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
        // 2026-08-05 변경: 이전엔 CorsConfig.java(WebMvcConfigurer)에도 똑같은 CORS
        // 설정이 따로 있어서 PATCH 하나만 여기 추가했다가 놓칠 뻔했음. Spring
        // Security를 쓰는 이상 실제로 적용되는 건 이 빈이므로, CorsConfig.java는
        // 삭제하고 이 메서드를 CORS 설정의 유일한 출처로 통일함.
        //
        // 2026-08-12 변경: 오리진을 "정확 일치"로만 받고 있어서, 로컬에서 Vite dev
        // 서버가 5173이 아닌 포트로 뜨면 API가 전부 막혔다. Vite는 지정 포트가 이미
        // 사용 중이면 조용히 5174, 5175로 올려 잡는데(dev 서버를 두 개 띄우면 바로
        // 이렇게 된다), 그러면 preflight가 403으로 떨어져 화면은 뜨는데 API만 전부
        // 실패하는 상태가 된다(2026-08-12 실측: 5173 → 200, 5174 → 403).
        //
        // 설정값에 '*'가 든 항목만 패턴으로 취급하고 나머지는 예전처럼 정확 일치로 둔다.
        // 전부 setAllowedOriginPatterns로 넘기지 않는 이유: setAllowedOrigins는 값이
        // "*"이면서 allowCredentials(true)이면 Spring이 예외로 막아주는데, 패턴 쪽은
        // 그 검사를 하지 않는다. 운영 설정(PRO_FRONTEND_ORIGIN)에 실수로 "*"가 들어가도
        // 조용히 통과하게 되므로, 그 안전장치는 남겨둔다.
        //
        // 로컬 설정은 application-local.yml에서 http://localhost:[*] 형태로 준다.
        // ⚠️ @Value의 String[] 바인딩이 쉼표로 자르므로 :[5173,5174] 같은 포트 목록
        //    문법은 쓸 수 없다. 포트 전체를 여는 :[*]를 쓸 것.
        List<String> exactOrigins = new ArrayList<>();
        List<String> originPatterns = new ArrayList<>();
        for (String origin : allowedOrigins) {
            String trimmed = origin.trim();
            if (trimmed.isEmpty()) continue;
            if (trimmed.contains("*")) originPatterns.add(trimmed);
            else exactOrigins.add(trimmed);
        }

        CorsConfiguration configuration = new CorsConfiguration();
        // 둘 다 설정할 수 있다. CorsConfiguration.checkOrigin이 정확 일치를 먼저 보고,
        // 걸리지 않으면 패턴을 본다.
        if (!exactOrigins.isEmpty()) configuration.setAllowedOrigins(exactOrigins);
        if (!originPatterns.isEmpty()) configuration.setAllowedOriginPatterns(originPatterns);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
