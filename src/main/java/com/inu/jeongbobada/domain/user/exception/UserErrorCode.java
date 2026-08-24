package com.inu.jeongbobada.domain.user.exception;

import com.inu.jeongbobada.global.exception.code.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum UserErrorCode implements BaseErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_404", "존재하지 않는 사용자입니다"), // 404
    DUPLICATE_STUDENT_ID(HttpStatus.CONFLICT, "USER_409", "이미 가입된 학번입니다"), // 409
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    UserErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
