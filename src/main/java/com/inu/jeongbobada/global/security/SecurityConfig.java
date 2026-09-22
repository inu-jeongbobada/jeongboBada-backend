package com.inu.jeongbobada.global.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // @PreAuthorize("hasRole('ADMIN')") 등 메서드 단위 권한 검사 활성화
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
public class SecurityConfig {

    private static final String[] PERMIT_ALL_PATHS = {
            "/api/auth/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
    };

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            JwtAccessDeniedHandler jwtAccessDeniedHandler,
            CorsConfigurationSource corsConfigurationSource
    ) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable) // stateless REST API라 CSRF 토큰 불필요
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex
                        // 인증 안 된 요청이 인증 필요한 경로에 접근하면 이 EntryPoint가 401 JSON을 응답
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        // 인증은 됐지만 권한(ADMIN 등)이 부족하면 이 핸들러가 403 JSON을 응답
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                )
                // 우리 필터를 Spring 기본 필터보다 먼저 실행 -> SecurityContext에 인증 정보를 먼저 채워둠
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PERMIT_ALL_PATHS).permitAll()
                        .requestMatchers("/api/users/me/**").authenticated()
                        .requestMatchers("/api/professors/*/professor-comments/**").authenticated()
                        // TODO: 그 외 인증이 필요한 경로(마이페이지 즐겨찾기 등)가 생기면
                        // 그 경로를 여기 authenticated()로 먼저 추가한 뒤 아래 anyRequest()보다 앞에 둘 것.
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    // 프론트(React, 별도 origin)에서의 호출을 허용. JWT는 Authorization 헤더로만 주고받고
    // 쿠키를 쓰지 않으므로 allowCredentials는 false로 둔다 (true면 allowedOrigins에 "*"를 못 쓰는 등 제약이 늘어남).
    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.allowedOrigins());
        configuration.setAllowedOriginPatterns(corsProperties.allowedOriginPatterns());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    //DB에 저장할때 암호화 된 형태로 저장
    //서비스에서 직접 생성하지 않아도 된다.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // AuthService에서 로그인 시 학번/비밀번호 검증을 직접 짜지 않고 여기에 위임하기 위한 빈
    // StudentUserDetailsService(UserDetailsService) + passwordEncoder 빈을 스프링이 알아서 엮어서 만들어줌
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }




}
