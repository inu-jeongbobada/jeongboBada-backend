package com.inu.jeongbobada.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordResetSendCodeRequest(
    @NotBlank(message = "학번을 입력해주세요")
    @Pattern(regexp = "\\d{9}", message = "학번은 숫자 9자리로 입력해주세요")
    String studentId,

    @NotBlank(message = "이메일을 입력해주세요")
    @Size(max = 100, message = "이메일은 100자 이하여야 합니다.")
    @Email(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "이메일 형식이 올바르지 않아요 (예: name@inu.ac.kr)")
    String email
) {
}
