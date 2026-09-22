package com.inu.jeongbobada.global.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

// application.yml의 cors.allowed-origins, cors.allowed-origin-patterns를 그대로 바인딩
@ConfigurationProperties(prefix = "cors")
public record CorsProperties(
    List<String> allowedOrigins,
    List<String> allowedOriginPatterns
) {
    public CorsProperties {
        // 프로필에 값이 없으면(예: application-test.properties) null이 아니라 빈 목록으로 — 미허용 origin은 기본적으로 다 막힌다
        allowedOrigins = allowedOrigins == null ? List.of() : allowedOrigins;
        allowedOriginPatterns = allowedOriginPatterns == null ? List.of() : allowedOriginPatterns;
    }
}
