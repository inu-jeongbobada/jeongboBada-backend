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
import java.util.Optional;

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

    // 서명/만료가 유효한 토큰의 학번. 위/변조되었거나 만료된 토큰이면 빈 값.
    // refresh token은 호출하는 쪽이 DB 저장값과 대조해야 한다.
    public Optional<String> resolveStudentId(String token) {
        return parseClaims(token).map(Claims::getSubject);
    }

    // 서명/만료에 더해 access token인지까지 확인한다. refresh token은 DB 대조 없이 통과하면
    // 로그아웃·비밀번호 변경으로 지워진 뒤에도 만료 전까지 API를 호출할 수 있기 때문이다.
    public Optional<String> resolveAccessTokenStudentId(String token) {
        return parseClaims(token)
            .filter(claims -> ACCESS_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_CLAIM)))
            .map(Claims::getSubject);
    }

    // 검증과 학번 추출을 한 번의 파싱으로 처리한다. 나눠서 두 번 파싱하면 그 사이에 토큰이 만료될 때
    // 검증을 통과한 뒤 예외가 나서 500이 된다.
    private Optional<Claims> parseClaims(String token) {
        try {
            return Optional.of(Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
