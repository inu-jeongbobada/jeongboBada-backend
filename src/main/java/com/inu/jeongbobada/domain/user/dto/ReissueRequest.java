package com.inu.jeongbobada.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

public record ReissueRequest(
    @NotBlank String refreshToken
) {
}
