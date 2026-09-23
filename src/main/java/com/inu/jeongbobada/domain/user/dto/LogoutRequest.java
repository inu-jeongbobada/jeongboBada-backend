package com.inu.jeongbobada.domain.user.dto;

import org.jspecify.annotations.Nullable;

// 로그아웃 시 서버에서 지울 refresh token. access token이 이미 만료돼도 이 값으로 로그아웃할 수 있다 (#129).
// 예전 방식(Authorization 헤더만 보내기)도 계속 받기 위해 선택 값이다.
public record LogoutRequest(
    @Nullable String refreshToken
) {
}
