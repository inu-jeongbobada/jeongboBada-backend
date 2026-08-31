package com.inu.jeongbobada.global.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    // 문자열 시크릿을 jjwt가 서명에 쓸 수 있는 SecretKey 형태로 변환
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(String studentId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.expiration());

        return Jwts.builder()
            .subject(studentId)        // 토큰의 주인이 누구인지 (나중에 파싱해서 studentId 꺼낼 때 씀)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(getSigningKey())
            .compact();
    }
}
