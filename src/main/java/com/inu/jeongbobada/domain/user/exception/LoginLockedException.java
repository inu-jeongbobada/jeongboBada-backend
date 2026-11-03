package com.inu.jeongbobada.domain.user.exception;

import com.inu.jeongbobada.global.exception.BusinessException;
import lombok.Getter;

// 프론트가 남은 시간을 표시할 수 있도록 Retry-After 헤더로 내려줄 값을 함께 담는다.
@Getter
public class LoginLockedException extends BusinessException {

    private final long retryAfterSeconds;

    public LoginLockedException(long retryAfterSeconds) {
        super(UserErrorCode.LOGIN_LOCKED);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
