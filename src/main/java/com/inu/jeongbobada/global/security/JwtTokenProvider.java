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

    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final JwtProperties jwtProperties;

    // 문자열 시크릿을 jjwt가 서명에 쓸 수 있는 SecretKey 형태로 변환
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(String studentId) {
        return createToken(studentId, ACCESS_TOKEN_TYPE, jwtProperties.expiration());
    }

    public String createRefreshToken(String studentId) {
        return createToken(studentId, REFRESH_TOKEN_TYPE, jwtProperties.refreshExpiration());
    }

    private String createToken(String studentId, String tokenType, long expirationMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
            .subject(studentId)
            .claim(TOKEN_TYPE_CLAIM, tokenType)
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

    // 서명/만료에 더해 access token인지까지 확인한다. refresh token은 DB 대조 없이 통과하면
    // 로그아웃·비밀번호 변경으로 지워진 뒤에도 만료 전까지 API를 호출할 수 있기 때문이다.
    public boolean validateAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
            return ACCESS_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_CLAIM, String.class));
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
