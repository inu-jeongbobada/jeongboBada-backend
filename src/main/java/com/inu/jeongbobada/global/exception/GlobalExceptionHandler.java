package com.inu.jeongbobada.global.exception;

import com.inu.jeongbobada.global.common.ApiResponse;
import com.inu.jeongbobada.global.exception.code.GlobalErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    // 등록되지 않은 경로 호출 (Spring 6.1+에서는 NoHandlerFoundException이 아니라 NoResourceFoundException)
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFoundException(NoResourceFoundException e) {
        log.warn("미등록 경로: {}", e.getResourcePath());

        ApiResponse<Void> response = ApiResponse.error(GlobalErrorCode.RESOURCE_NOT_FOUND);
        return ResponseEntity.status(response.httpStatus()).body(response);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowedException(HttpRequestMethodNotSupportedException e) {
        ApiResponse<Void> response = ApiResponse.error(GlobalErrorCode.METHOD_NOT_ALLOWED);
        return ResponseEntity.status(response.httpStatus()).body(response);
    }

    // 요청 바디가 비어있거나 JSON 파싱이 안 될 때 (형식 자체가 깨진 경우)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("잘못된 요청 바디: {}", e.getMessage());

        ApiResponse<Void> response = ApiResponse.error(GlobalErrorCode.INVALID_INPUT_VALUE);
        return ResponseEntity.status(response.httpStatus()).body(response);
    }

    // 경로/쿼리 파라미터 타입이 안 맞을 때 (예: /api/users/abc 처럼 숫자 자리에 문자열)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("파라미터 타입 불일치: {}", e.getName());

        ApiResponse<Void> response = ApiResponse.error(GlobalErrorCode.INVALID_INPUT_VALUE);
        return ResponseEntity.status(response.httpStatus()).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unhandled exception", e);

        ApiResponse<Void> response = ApiResponse.error(GlobalErrorCode.INTERNAL_SERVER_ERROR);
        return ResponseEntity.status(response.httpStatus()).body(response);
    }
}
