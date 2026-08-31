package com.inu.jeongbobada.global.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

// application.yml의 jwt.secret, jwt.expiration을 그대로 바인딩
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
    String secret,
    long expiration
) {
}
