package com.inu.jeongbobada.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
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
        return createToken(studentId, jwtProperties.expiration());
    }

    public String createRefreshToken(String studentId) {
        return createToken(studentId, jwtProperties.refreshExpiration());
    }

    private String createToken(String studentId, long expirationMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
            .subject(studentId)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(getSigningKey())
            .compact();
    }

    //아래 두 메서드는 사실상 세트이다?
    // 1. 정상토큰인지 확인
    // 2. 학번 추출
    // 서명/만료 검증. 위/변조되었거나 만료된 토큰이면 false
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    //쉽게 말하면 토큰의 주인을 찾는 메서드?
    public String getStudentId(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
        return claims.getSubject();
    }


}
