package com.inu.jeongbobada.global.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

// application.yml의 jwt.secret, jwt.expiration을 그대로 바인딩
// @Validated: 값이 비었거나 잘못되면 서버가 기동 단계에서 실패한다. 검증이 없으면 서버는 정상으로 뜨고
// 첫 로그인에서야 500이 나서 원인 찾기가 어렵다 (docker compose에서 JWT_SECRET을 빼먹은 경우 등, #93).
@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
    // HS256 서명 키는 32바이트(256비트) 이상이어야 한다. 더 짧으면 jjwt가 토큰 발급 때 예외를 던진다.
    @NotBlank(message = "jwt.secret(JWT_SECRET)이 비어 있습니다. .env 또는 application.yml에 32자 이상의 값을 넣으세요")
    @Size(min = 32, message = "jwt.secret(JWT_SECRET)은 32자 이상이어야 합니다 (HS256)")
    String secret,
    @Positive long expiration,
    @Positive long refreshExpiration
) {
}
