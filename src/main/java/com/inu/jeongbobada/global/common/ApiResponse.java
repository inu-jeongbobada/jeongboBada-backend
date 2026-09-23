package com.inu.jeongbobada.global.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.inu.jeongbobada.global.exception.code.BaseErrorCode;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;

import java.util.List;

//CI 설정을 해놓아서 이걸 없애면 CI 통과가 안된다. 나중에 생각
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        @JsonIgnore
        HttpStatus httpStatus,
        boolean success,
        @Nullable T data,
        @Nullable String code,
        @Nullable String message,
        // 검증 실패처럼 필드 단위로 알려줄 게 있을 때만 채운다 (#111). null이면 JSON에서 빠진다.
        @Nullable List<FieldError> errors
) {

    // 프론트가 폼의 어느 칸에 오류를 표시할지 알 수 있도록 필드 이름과 사유를 함께 내려준다.
    public record FieldError(@Nullable String field, String message) {
    }

    public static <T> ApiResponse<T> ok(@Nullable final T data) {
        return new ApiResponse<>(HttpStatus.OK, true, data, null, null, null);
    }

    public static <T> ApiResponse<T> created(@Nullable final T data) {
        return new ApiResponse<>(HttpStatus.CREATED, true, data, null, null, null);
    }

    // 에러 응답은 반드시 BaseErrorCode를 거친다 — code 없는 에러 응답을 만들 수 없게 (#131)
    public static <T> ApiResponse<T> error(final BaseErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getHttpStatus(), false, null, errorCode.getCode(), errorCode.getMessage(), null);
    }

    // @Valid 검증 실패처럼, ErrorCode의 code/status는 유지하되 메시지만 상황에 맞게 바꿔야 할 때 사용
    public static <T> ApiResponse<T> error(final BaseErrorCode errorCode, final String message) {
        return new ApiResponse<>(errorCode.getHttpStatus(), false, null, errorCode.getCode(), message, null);
    }

    // 필드별 오류를 함께 내려줄 때. message는 대표 문구(첫 번째 오류), errors는 전체 목록.
    public static <T> ApiResponse<T> error(final BaseErrorCode errorCode, final String message, final List<FieldError> errors) {
        return new ApiResponse<>(errorCode.getHttpStatus(), false, null, errorCode.getCode(), message, errors);
    }
}
