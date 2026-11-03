package com.inu.jeongbobada.global.exception;

import com.inu.jeongbobada.global.exception.code.BaseErrorCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final BaseErrorCode errorCode;

    public BusinessException(BaseErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    // code와 상태는 그대로 두고, 상황에 맞는 값을 문구에 넣어야 할 때 사용
    public BusinessException(BaseErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
