package com.inu.jeongbobada.domain.user.dto;

public record TokenResponse(
    String accessToken,
    String refreshToken
) {
}
