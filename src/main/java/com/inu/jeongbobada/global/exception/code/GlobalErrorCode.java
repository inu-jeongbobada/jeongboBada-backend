package com.inu.jeongbobada.global.exception.code;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum GlobalErrorCode implements BaseErrorCode {

    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON_400", "잘못된 입력값입니다"), // 400
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON_405", "지원하지 않는 HTTP 메서드입니다"), // 405
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "서버 내부 오류가 발생했습니다"), // 500
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    GlobalErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
