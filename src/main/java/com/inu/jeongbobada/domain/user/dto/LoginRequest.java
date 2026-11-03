package com.inu.jeongbobada.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(
    @NotBlank(message = "학번을 입력해주세요")
    @Pattern(regexp = "\\d{9}", message = "학번은 숫자 9자리로 입력해주세요")
    String studentId,

    @NotBlank(message = "비밀번호를 입력해주세요")
    String password
) {
}
