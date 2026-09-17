package com.inu.jeongbobada.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank String studentId,
    @NotBlank String password
) {
}
