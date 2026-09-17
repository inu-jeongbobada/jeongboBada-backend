package com.inu.jeongbobada.domain.professorComment.exception;

import com.inu.jeongbobada.global.exception.code.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ProfessorCommentErrorCode implements BaseErrorCode {
    PROFESSOR_COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PROFESSOR_COMMENT_404", "존재하지 않는 교수 후기입니다."),
    PROFESSOR_COMMENT_FORBIDDEN(HttpStatus.FORBIDDEN, "PROFESSOR_COMMENT_403", "교수 후기를 수정할 권한이 없습니다."),
    INVALID_PROFESSOR(HttpStatus.UNAUTHORIZED, "PROFESSOR_COMMENT_401", "일치하지 않는 교수입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ProfessorCommentErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
