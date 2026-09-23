package com.inu.jeongbobada.global.exception;

import com.inu.jeongbobada.domain.user.exception.UserErrorCode;
import com.inu.jeongbobada.global.common.ApiResponse;
import com.inu.jeongbobada.global.exception.code.GlobalErrorCode;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.sql.SQLException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // MySQL ER_DUP_ENTRY: UNIQUE 제약 위반
    private static final int MYSQL_DUPLICATE_ENTRY = 1062;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("BusinessException: {}", e.getMessage());

        ApiResponse<Void> response = ApiResponse.error(e.getErrorCode());
        return ResponseEntity.status(response.httpStatus()).body(response);
    }

    // 학번이 없는 경우/비밀번호가 틀린 경우를 구분해서 응답하면
    // 공격자가 "이 학번은 가입돼있다"를 알아낼 수 있어서(user enumeration) 메시지를 하나로 통일함
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(BadCredentialsException e) {
        log.warn("로그인 실패: {}", e.getMessage());

        ApiResponse<Void> response = ApiResponse.error(UserErrorCode.INVALID_CREDENTIALS);
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

    // 엔티티 저장 시점의 Bean Validation 실패도 잘못된 입력값(400)으로 응답한다.
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getMessage())
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

    // @RequestHeader로 필수 지정한 헤더가 아예 안 왔을 때 (예: Authorization 헤더 누락)
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingRequestHeaderException(MissingRequestHeaderException e) {
        log.warn("필수 헤더 누락: {}", e.getHeaderName());

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

    // 필수 쿼리 파라미터가 아예 안 왔을 때 (예: GET /api/auth/check-nickname 에 nickname 누락)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameterException(MissingServletRequestParameterException e) {
        log.warn("필수 파라미터 누락: {}", e.getParameterName());

        ApiResponse<Void> response = ApiResponse.error(GlobalErrorCode.INVALID_INPUT_VALUE);
        return ResponseEntity.status(response.httpStatus()).body(response);
    }

    // JSON을 받는 API에 Content-Type이 없거나 text/plain 등으로 보냈을 때
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        log.warn("지원하지 않는 Content-Type: {}", e.getContentType());

        ApiResponse<Void> response = ApiResponse.error(GlobalErrorCode.UNSUPPORTED_MEDIA_TYPE);
        return ResponseEntity.status(response.httpStatus()).body(response);
    }

    // Accept가 application/xml처럼 JSON으로 응답할 수 없는 형식일 때.
    // 그냥 body를 돌려주면 이 에러 응답도 같은 Accept 때문에 직렬화에 실패해 빈 body가 나간다.
    // Content-Type을 JSON으로 미리 지정하면 Spring이 Accept 협상을 건너뛰고 JSON으로 쓴다.
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotAcceptableException(HttpMediaTypeNotAcceptableException e) {
        ApiResponse<Void> response = ApiResponse.error(GlobalErrorCode.NOT_ACCEPTABLE);
        return ResponseEntity.status(response.httpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    // 서비스의 사전 중복 확인(findBy...)을 통과했지만 저장 순간 다른 요청이 먼저 넣어서 UNIQUE 제약에 걸린 경우 (동시 요청).
    // 어떤 필드가 겹쳤는지는 알려주지 않는다 — 더블클릭이면 첫 요청이 이미 성공했고, 도메인 제약 이름을 global이 알 필요도 없다.
    // UNIQUE가 아닌 무결성 위반(NOT NULL, FK 등)은 클라이언트가 고칠 수 있는 문제가 아니라 서버 버그라 500으로 둔다.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        if (!isDuplicateKey(e)) {
            log.error("데이터 무결성 위반", e);
            ApiResponse<Void> response = ApiResponse.error(GlobalErrorCode.INTERNAL_SERVER_ERROR);
            return ResponseEntity.status(response.httpStatus()).body(response);
        }
        log.warn("UNIQUE 제약 위반(동시 요청): {}", e.getMostSpecificCause().getMessage());

        ApiResponse<Void> response = ApiResponse.error(GlobalErrorCode.DUPLICATE_RESOURCE);
        return ResponseEntity.status(response.httpStatus()).body(response);
    }

    private static boolean isDuplicateKey(DataIntegrityViolationException e) {
        if (e instanceof DuplicateKeyException) {
            return true;
        }
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SQLException sqlException && sqlException.getErrorCode() == MYSQL_DUPLICATE_ENTRY) {
                return true;
            }
        }
        return false;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unhandled exception", e);

        ApiResponse<Void> response = ApiResponse.error(GlobalErrorCode.INTERNAL_SERVER_ERROR);
        return ResponseEntity.status(response.httpStatus()).body(response);
    }


}
