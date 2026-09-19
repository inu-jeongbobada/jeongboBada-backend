package com.inu.jeongbobada.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PasswordResetVerifyCodeRequest(
    @NotBlank(message = "학번은 필수입니다.")
    @Pattern(regexp = "\\d{9}", message = "학번은 숫자 9자리여야 합니다.")
    String studentId,

    @NotBlank(message = "인증코드는 필수입니다.")
    @Pattern(regexp = "\\d{6}", message = "인증코드는 숫자 6자리여야 합니다.")
    String code
) {
}
