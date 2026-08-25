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
}
