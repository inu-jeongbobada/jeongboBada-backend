package com.inu.jeongbobada.global.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    // HS256 최소 요구 길이(32바이트)를 넉넉히 넘기는 테스트 전용 시크릿
    private static final String SECRET = "test-secret-key-for-jwt-token-provider-unit-test-0123456789";
    private static final String STUDENT_ID = "202012345";

    private JwtTokenProvider tokenProvider(long expirationMs, long refreshExpirationMs) {
        return new JwtTokenProvider(new JwtProperties(SECRET, expirationMs, refreshExpirationMs));
    }

    @Test
    void access_token은_양쪽_모두에서_학번을_돌려준다() {
        // given
        JwtTokenProvider provider = tokenProvider(60_000L, 120_000L);

        // when
        String token = provider.createAccessToken(STUDENT_ID);

        // then
        assertThat(provider.resolveStudentId(token)).contains(STUDENT_ID);
        assertThat(provider.resolveAccessTokenStudentId(token)).contains(STUDENT_ID);
    }

    @Test
    void refresh_token은_access_token으로_인정하지_않는다() {
        // given
        JwtTokenProvider provider = tokenProvider(60_000L, 120_000L);

        // when
        String token = provider.createRefreshToken(STUDENT_ID);

        // then
        assertThat(provider.resolveStudentId(token)).contains(STUDENT_ID);
        assertThat(provider.resolveAccessTokenStudentId(token)).isEmpty();
    }

    @Test
    void type_claim이_없는_예전_토큰은_access_token으로_인정하지_않지만_학번은_읽을_수_있다() {
        // given: type claim 도입 전에 발급된 토큰. refresh는 재발급 API에서 계속 쓸 수 있어야 한다
        JwtTokenProvider provider = tokenProvider(60_000L, 120_000L);

        // when
        String legacyToken = tokenWithClaim(null);

        // then
        assertThat(provider.resolveStudentId(legacyToken)).contains(STUDENT_ID);
        assertThat(provider.resolveAccessTokenStudentId(legacyToken)).isEmpty();
    }

    @Test
    void type_claim이_문자열이_아니어도_예외없이_access_token으로_인정하지_않는다() {
        // given
        JwtTokenProvider provider = tokenProvider(60_000L, 120_000L);

        // when & then
        assertThat(provider.resolveAccessTokenStudentId(tokenWithClaim(123))).isEmpty();
    }

    @Test
    void 만료된_토큰은_예외없이_빈_값이다() {
        // given: 만료시간을 음수로 줘서 발급 즉시 만료된 토큰을 만든다
        JwtTokenProvider provider = tokenProvider(-1_000L, -1_000L);

        // when
        String expiredToken = provider.createAccessToken(STUDENT_ID);

        // then
        assertThat(provider.resolveStudentId(expiredToken)).isEmpty();
        assertThat(provider.resolveAccessTokenStudentId(expiredToken)).isEmpty();
    }

    @Test
    void 변조된_토큰은_예외없이_빈_값이다() {
        // given
        JwtTokenProvider provider = tokenProvider(60_000L, 120_000L);
        String token = provider.createAccessToken(STUDENT_ID);

        // when: 서명 검증에 실패하도록 payload 마지막 글자를 하나 바꿔치기
        String tampered = token.substring(0, token.length() - 1)
            + (token.charAt(token.length() - 1) == 'a' ? 'b' : 'a');

        // then
        assertThat(provider.resolveStudentId(tampered)).isEmpty();
        assertThat(provider.resolveAccessTokenStudentId(tampered)).isEmpty();
    }

    @Test
    void 형식이_깨진_문자열은_예외없이_빈_값이다() {
        // given
        JwtTokenProvider provider = tokenProvider(60_000L, 120_000L);

        // when & then
        assertThat(provider.resolveStudentId("이건-토큰이-아님")).isEmpty();
        assertThat(provider.resolveAccessTokenStudentId("이건-토큰이-아님")).isEmpty();
    }

    // JwtTokenProvider로는 만들 수 없는 type claim을 가진, 서명이 유효한 토큰
    private String tokenWithClaim(Object type) {
        var builder = Jwts.builder()
            .subject(STUDENT_ID)
            .expiration(new Date(System.currentTimeMillis() + 60_000L))
            .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)));
        if (type != null) {
            builder.claim("type", type);
        }
        return builder.compact();
    }
}
