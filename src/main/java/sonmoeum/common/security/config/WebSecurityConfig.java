package sonmoeum.common.security.config;

import java.time.Duration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import sonmoeum.common.security.handler.CustomAccessDeniedHandler;
import sonmoeum.common.security.handler.CustomAuthenticationEntryPoint;
import sonmoeum.common.security.jwt.JwtAuthenticationFilter;
import sonmoeum.common.security.oauth.CookieOAuth2AuthorizationRequestRepository;
import sonmoeum.common.security.oauth.OAuth2SuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    // Stateless 구성에 필요한 컴포넌트들
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CookieOAuth2AuthorizationRequestRepository cookieOAuth2AuthorizationRequestRepository;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String corsAllowedOrigins;

    @Bean
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
                // 문서/헬스체크
                .requestMatchers("/actuator/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/swagger-ui").permitAll()
                // redirect 페이지(정적)
                .requestMatchers("/oauth2/redirect/**").permitAll()
                // 자체 인증 API 경로
                .requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                // OAuth2 로그인 플로우 엔드포인트
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                // 그 외는 인증 필요
                .anyRequest().authenticated()
            )

            // OAuth2 로그인 (세션 없이: AuthorizationRequest를 쿠키로 저장)
            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(authorization -> authorization
                    .authorizationRequestRepository(cookieOAuth2AuthorizationRequestRepository)
                )
                .successHandler(oAuth2SuccessHandler)
            )
            // JWT 인증 필터: UsernamePasswordAuthenticationFilter 전에 둠
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

            .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // credentials=true 이면 allowedOrigins에 "*" 불가
        config.setAllowCredentials(true);

        List<String> origins = Arrays.stream(corsAllowedOrigins.split(","))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .collect(Collectors.toList());

        // 혹시 "*"가 들어오면(실수 방지) credentials를 꺼버리는 방어 로직
        if (origins.contains("*")) {
            config.setAllowCredentials(false);
            config.addAllowedOrigin("*");
        } else {
            // Spring Security 6 / Boot 3.x에서 패턴을 쓸 경우 addAllowedOriginPattern 사용 가능
            // 여기서는 명시 도메인만 허용
            origins.forEach(config::addAllowedOrigin);
        }

        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        // OAuth2 리다이렉트/쿠키 동작 고려 시 캐시 시간은 적당히
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
}
