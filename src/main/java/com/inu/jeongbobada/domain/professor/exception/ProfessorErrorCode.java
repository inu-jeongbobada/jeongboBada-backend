package com.inu.jeongbobada.domain.professor.exception;

import com.inu.jeongbobada.global.exception.code.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ProfessorErrorCode implements BaseErrorCode {
    PROFESSOR_NOT_FOUND(HttpStatus.NOT_FOUND, "PROFESSOR_404", "존재하지 않는 교수입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ProfessorErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
