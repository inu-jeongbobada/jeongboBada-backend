package com.inu.jeongbobada.global.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    // HS256 최소 요구 길이(32바이트)를 넉넉히 넘기는 테스트 전용 시크릿
    private static final String SECRET = "test-secret-key-for-jwt-token-provider-unit-test-0123456789";

    private JwtTokenProvider tokenProvider(long expirationMs, long refreshExpirationMs) {
        return new JwtTokenProvider(new JwtProperties(SECRET, expirationMs, refreshExpirationMs));
    }

    @Test
    void 정상_토큰은_검증을_통과하고_학번을_그대로_추출한다() {
        // given
        JwtTokenProvider provider = tokenProvider(60_000L, 120_000L);
        String studentId = "202012345";

        // when
        String token = provider.createAccessToken(studentId);

        // then
        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getStudentId(token)).isEqualTo(studentId);
    }

    @Test
    void refreshToken도_학번을_그대로_추출한다() {
        // given
        JwtTokenProvider provider = tokenProvider(60_000L, 120_000L);
        String studentId = "202012345";

        // when
        String token = provider.createRefreshToken(studentId);

        // then
        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getStudentId(token)).isEqualTo(studentId);
    }

    @Test
    void 만료된_토큰은_검증에_실패한다() {
        // given: 만료시간을 음수로 줘서 발급 즉시 만료된 토큰을 만든다
        JwtTokenProvider provider = tokenProvider(-1_000L, -1_000L);

        // when
        String expiredToken = provider.createAccessToken("202012345");

        // then
        assertThat(provider.validateToken(expiredToken)).isFalse();
    }

    @Test
    void 변조된_토큰은_검증에_실패한다() {
        // given
        JwtTokenProvider provider = tokenProvider(60_000L, 120_000L);
        String token = provider.createAccessToken("202012345");

        // when: 서명 검증에 실패하도록 payload 마지막 글자를 하나 바꿔치기
        String tampered = token.substring(0, token.length() - 1)
            + (token.charAt(token.length() - 1) == 'a' ? 'b' : 'a');

        // then
        assertThat(provider.validateToken(tampered)).isFalse();
    }

    @Test
    void 형식이_깨진_문자열은_검증에_실패한다() {
        // given
        JwtTokenProvider provider = tokenProvider(60_000L, 120_000L);

        // when & then
        assertThat(provider.validateToken("이건-토큰이-아님")).isFalse();
    }
}
