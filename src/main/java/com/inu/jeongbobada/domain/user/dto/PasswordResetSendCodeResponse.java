package com.inu.jeongbobada.domain.user.dto;

// 재발송 토스트의 (n/5회) 표시용. 그날의 첫 발송이면 resendCount는 0이다.
public record PasswordResetSendCodeResponse(int resendCount, int maxResendCount) {
}
