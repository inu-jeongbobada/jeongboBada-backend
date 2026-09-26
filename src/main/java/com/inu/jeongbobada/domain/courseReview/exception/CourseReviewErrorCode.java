package com.inu.jeongbobada.domain.courseReview.exception;

import com.inu.jeongbobada.global.exception.code.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CourseReviewErrorCode implements BaseErrorCode {

    // 후기 단위는 (사용자, 과목, 교수) — UK_USER_COURSE_PROFESSOR_REVIEW
    DUPLICATE_COURSE_REVIEW(HttpStatus.CONFLICT, "DUPLICATE_COURSE_REVIEW", "이미 이 강의에 후기를 작성했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    CourseReviewErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
