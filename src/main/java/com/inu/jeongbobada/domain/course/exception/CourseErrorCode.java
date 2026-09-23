package com.inu.jeongbobada.domain.course.exception;

import com.inu.jeongbobada.global.exception.code.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CourseErrorCode implements BaseErrorCode {

    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "COURSE_NOT_FOUND", "과목을 찾을 수 없습니다."),
    COURSE_OFFERING_NOT_FOUND(HttpStatus.NOT_FOUND, "COURSE_OFFERING_NOT_FOUND", "해당 교수가 개설한 과목이 아닙니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    CourseErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
