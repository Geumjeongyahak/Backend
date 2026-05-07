package geumjeongyahak.common.security.config;

import java.time.Duration;

import java.util.Arrays;
import java.util.List;
import geumjeongyahak.common.security.handler.CustomAccessDeniedHandler;
import geumjeongyahak.common.security.handler.CustomAuthenticationEntryPoint;
import geumjeongyahak.common.security.jwt.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import geumjeongyahak.common.security.service.PermissionCodeEvaluator;
import org.springframework.http.HttpMethod;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
@EnableConfigurationProperties(SecurityProperties.class)
public class WebSecurityConfig {

    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String corsAllowedOrigins;

    @Bean
    @Order(1)
    public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
        return http
            .securityMatcher("/admin/**")
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/admin/auth/login").permitAll()
                .anyRequest().hasAnyRole("ADMIN", "MANAGER")
            )
            .formLogin(form -> form
                .loginPage("/admin/auth/login")
                .loginProcessingUrl("/admin/auth/login")
                .defaultSuccessUrl("/admin", true)
                .failureUrl("/admin/auth/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/admin/auth/logout")
                .logoutSuccessUrl("/admin/auth/login?logout")
            )
            .exceptionHandling(exception -> exception
                .accessDeniedPage("/admin/auth/login?denied")
            )
            .build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            // 기본 인증 방식 비활성화 (API 서버 기준)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)

            // CSRF: JWT(Bearer) 기반 -> disable
            .csrf(AbstractHttpConfigurer::disable)

            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // 세션 완전 차단(Stateless)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 개발 편의 (H2 콘솔 등) 필요하면 유지
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))

            // 예외 처리(기존 커스텀 핸들러 유지)
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )

            // 인가 정책: 화이트리스트만 permitAll + 나머지 authenticated
            .authorizeHttpRequests(auth -> auth
                // 관리자 로그인 페이지로 redirect 용
                .requestMatchers(HttpMethod.GET, "/").permitAll()
                // 정적 리소스 및 문서/헬스체크
                .requestMatchers("/favicon.ico", "/icons/**", "/site.webmanifest", "/actuator/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/swagger-ui").permitAll()
                // 인증 API (로그인, 회원가입, 토큰 재발급, 로그아웃)
                .requestMatchers("/api/v1/auth/login", "/api/v1/auth/signup", "/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll()
                .requestMatchers("/api/v1/auth/google/**").permitAll()
                // 공개 조회 API (목록 위주)
                .requestMatchers(HttpMethod.GET, "/api/v1/classrooms").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/departments").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/channels").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/channels/*/posts", "/api/v1/channels/*/posts/**").permitAll()
                // 그 외는 인증 필요
                .anyRequest().authenticated()
            )

            // JWT 인증 필터: UsernamePasswordAuthenticationFilter 전에 둠
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

            .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowCredentials(false);

        List<String> origins = Arrays.stream(corsAllowedOrigins.split(","))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .toList();

        origins.forEach(config::addAllowedOrigin);

        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        // CORS preflight(OPTIONS) 캐시 시간
        config.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
        AuthenticationConfiguration authenticationConfiguration
    ) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler(PermissionCodeEvaluator permissionCodeEvaluator) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(permissionCodeEvaluator);
        return handler;
    }
}
