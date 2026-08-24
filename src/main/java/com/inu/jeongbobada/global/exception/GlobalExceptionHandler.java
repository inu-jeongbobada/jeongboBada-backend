package com.inu.jeongbobada.global.exception;

import com.inu.jeongbobada.global.common.ApiResponse;
import com.inu.jeongbobada.global.exception.code.GlobalErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("BusinessException: {}", e.getMessage());

        ApiResponse<Void> response = ApiResponse.error(e.getErrorCode());
        return ResponseEntity.status(response.httpStatus()).body(response);
    }

    // @Valid 검증 실패 시, 첫 번째 필드 에러 메시지를 응답으로 내려줌
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getDefaultMessage())
                .orElse(GlobalErrorCode.INVALID_INPUT_VALUE.getMessage());

        ApiResponse<Void> response = ApiResponse.error(GlobalErrorCode.INVALID_INPUT_VALUE, message);
        return ResponseEntity.status(response.httpStatus()).body(response);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowedException(HttpRequestMethodNotSupportedException e) {
        ApiResponse<Void> response = ApiResponse.error(GlobalErrorCode.METHOD_NOT_ALLOWED);
        return ResponseEntity.status(response.httpStatus()).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unhandled exception", e);

        ApiResponse<Void> response = ApiResponse.error(GlobalErrorCode.INTERNAL_SERVER_ERROR);
        return ResponseEntity.status(response.httpStatus()).body(response);
    }
}
