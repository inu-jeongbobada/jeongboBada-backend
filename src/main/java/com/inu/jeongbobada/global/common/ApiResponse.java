package com.inu.jeongbobada.global.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.inu.jeongbobada.global.exception.code.BaseErrorCode;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;

//CI 설정을 해놓아서 이걸 없애면 CI 통과가 안된다. 나중에 생각
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        @JsonIgnore
        HttpStatus httpStatus,
        boolean success,
        @Nullable T data,
        @Nullable String code,
        @Nullable String message
) {

    public static <T> ApiResponse<T> ok(@Nullable final T data) {
        return new ApiResponse<>(HttpStatus.OK, true, data, null, null);
    }

    public static <T> ApiResponse<T> created(@Nullable final T data) {
        return new ApiResponse<>(HttpStatus.CREATED, true, data, null, null);
    }

    //지울 예정
    public static <T> ApiResponse<T> error(final HttpStatus httpStatus, final String message) {
        return new ApiResponse<>(httpStatus, false, null, null, message);
    }

    public static <T> ApiResponse<T> error(final BaseErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getHttpStatus(), false, null, errorCode.getCode(), errorCode.getMessage());
    }

    // @Valid 검증 실패처럼, ErrorCode의 code/status는 유지하되 메시지만 상황에 맞게 바꿔야 할 때 사용
    public static <T> ApiResponse<T> error(final BaseErrorCode errorCode, final String message) {
        return new ApiResponse<>(errorCode.getHttpStatus(), false, null, errorCode.getCode(), message);
    }
}
