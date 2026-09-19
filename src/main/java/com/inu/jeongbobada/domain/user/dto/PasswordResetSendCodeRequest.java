package com.inu.jeongbobada.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordResetSendCodeRequest(
    @NotBlank(message = "학번은 필수입니다.")
    @Pattern(regexp = "\\d{9}", message = "학번은 숫자 9자리여야 합니다.")
    String studentId,

    @NotBlank(message = "이메일은 필수입니다.")
    @Size(max = 100, message = "이메일은 100자 이하여야 합니다.")
    @Email(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "올바른 이메일 형식이 아닙니다.")
    String email
) {
}
