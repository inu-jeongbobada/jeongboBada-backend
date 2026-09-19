package com.inu.jeongbobada.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// 서버는 verify-code 통과 여부를 기억하지 않으므로, 재설정 요청에도 인증코드를 다시 담아 보내야 한다.
public record PasswordResetRequest(
    @NotBlank(message = "학번은 필수입니다.")
    @Pattern(regexp = "\\d{9}", message = "학번은 숫자 9자리여야 합니다.")
    String studentId,

    @NotBlank(message = "인증코드는 필수입니다.")
    @Pattern(regexp = "\\d{6}", message = "인증코드는 숫자 6자리여야 합니다.")
    String code,

    @NotBlank(message = "새 비밀번호는 필수입니다.")
    @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다.")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
        message = "비밀번호는 영문과 숫자를 각각 최소 1개 포함해야 합니다."
    )
    String newPassword
) {
}
