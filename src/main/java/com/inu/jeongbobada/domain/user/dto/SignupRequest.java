package com.inu.jeongbobada.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
    @NotBlank(message = "학번은 필수입니다.")
    @Pattern(
            regexp = "\\d{9}",
            message = "학번은 숫자 9자리여야 합니다."
    )
    String studentId,

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다.")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
            message = "비밀번호는 영문과 숫자를 각각 최소 1개 포함해야 합니다."
    )
    String password,

    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = 2, max = 10, message = "닉네임은 2자 이상 10자 이하여야 합니다.")
    @Pattern(
        regexp = "^[가-힣a-zA-Z0-9_-]+$",
        message = "닉네임은 한글, 영문, 숫자, '_', '-'만 사용할 수 있습니다."
    )
    String nickname
) {
}
