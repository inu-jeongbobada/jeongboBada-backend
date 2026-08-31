package com.inu.jeongbobada.domain.course.exception;

import com.inu.jeongbobada.global.exception.code.BaseErrorCode;
import org.springframework.http.HttpStatus;

public enum CourseException implements BaseErrorCode {

    COURSE_NOT_FOUND("COURSE_4041", "과목을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus status;

    CourseException(String code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return status;
    }
}
