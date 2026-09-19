package com.inu.jeongbobada.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmailUpdateRequest(
    @NotBlank(message = "새 이메일은 필수입니다.")
    @Size(max = 100, message = "이메일은 100자 이하여야 합니다.")
    @Email(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "올바른 이메일 형식이 아닙니다.")
    String newEmail,

    @NotBlank(message = "인증코드는 필수입니다.")
    @Pattern(regexp = "\\d{6}", message = "인증코드는 숫자 6자리여야 합니다.")
    String code,

    @NotBlank(message = "현재 비밀번호는 필수입니다.")
    String currentPassword
) {
}
