package com.inu.jeongbobada.global.exception;

import com.inu.jeongbobada.domain.user.exception.UserErrorCode;
import com.inu.jeongbobada.global.common.ApiResponse;
import com.inu.jeongbobada.global.common.ApiResponse.FieldError;
import com.inu.jeongbobada.global.exception.code.BaseErrorCode;
import com.inu.jeongbobada.global.exception.code.GlobalErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.MismatchedInputException;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

// Spring MVC 표준 예외(415·406·405·404·파라미터 누락·파싱 실패·타입 오류 등 약 20종)는
// 부모(ResponseEntityExceptionHandler)가 알맞은 상태 코드로 처리하고, 전부 handleExceptionInternal로 모인다.
// 여기서 ApiResponse 형식으로 감싸므로, 표준 예외를 하나씩 찾아 핸들러를 추가하지 않아도 500으로 새지 않는다 (#109, #111).
// 주의: 부모가 이미 처리하는 예외 타입에 @ExceptionHandler를 또 달면 기동 시 ambiguous 오류가 난다.
//       메시지를 바꾸고 싶으면 부모의 handleXxx 메서드를 오버라이드할 것.
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // MySQL ER_DUP_ENTRY: UNIQUE 제약 위반
    private static final int MYSQL_DUPLICATE_ENTRY = 1062;

    // ---------- 우리 코드가 던지는 예외 ----------

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

    // 엔티티 저장 시점의 Bean Validation 실패(ConstraintViolationException)도 여기서 500 — 잘못된 엔티티는 서버 버그다.
    // 클라이언트 입력 검증은 요청 DTO + @Valid(MethodArgumentNotValidException, 400)가 맡는다 (#139)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unhandled exception", e);

        ApiResponse<Void> response = ApiResponse.error(GlobalErrorCode.INTERNAL_SERVER_ERROR);
        return ResponseEntity.status(response.httpStatus()).body(response);
    }

    // ---------- Spring MVC 표준 예외: 메시지를 구체적으로 바꾸는 것만 오버라이드 ----------

    // @Valid 검증 실패. message는 기존처럼 첫 번째 오류 문구를 유지하고(하위 호환),
    // errors에 실패한 필드를 전부 담는다.
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldError> errors = ex.getBindingResult().getAllErrors().stream()
                .map(error -> new FieldError(
                        error instanceof org.springframework.validation.FieldError fieldError ? fieldError.getField() : null,
                        error.getDefaultMessage() != null ? error.getDefaultMessage() : GlobalErrorCode.INVALID_INPUT_VALUE.getMessage()))
                .toList();

        return handleExceptionInternal(ex, invalidInput(errors), headers, status, request);
    }

    // @RequestParam 등에 붙은 검증(@Size 등)이 실패했을 때 (Spring 6.1+ 메서드 검증)
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldError> errors = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new FieldError(
                                result.getMethodParameter().getParameterName(),
                                error.getDefaultMessage() != null ? error.getDefaultMessage() : GlobalErrorCode.INVALID_INPUT_VALUE.getMessage())))
                .toList();

        return handleExceptionInternal(ex, invalidInput(errors), headers, status, request);
    }

    // 요청 body가 없거나 JSON이 깨졌거나, 필드 값의 타입이 틀렸을 때 (예: "rating":"SIX").
    // 원문 예외 메시지에는 내부 클래스명이 들어 있어서 그대로 내보내지 않고, 필드 경로와 허용 값만 뽑아 쓴다.
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.warn("잘못된 요청 바디: {}", ex.getMessage());

        MismatchedInputException mismatch = findCause(ex, MismatchedInputException.class);
        String field = mismatch != null ? jsonPath(mismatch.getPath()) : "";
        ApiResponse<Void> body;
        if (field.isEmpty()) {
            body = ApiResponse.error(GlobalErrorCode.INVALID_INPUT_VALUE, "요청 본문이 비어 있거나 JSON 형식이 올바르지 않습니다");
        } else {
            String message = invalidValueMessage(field, mismatch.getTargetType());
            body = ApiResponse.error(GlobalErrorCode.INVALID_INPUT_VALUE, message, List.of(new FieldError(field, message)));
        }
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    // 경로 변수·쿼리 파라미터 타입이 틀렸을 때 (예: /api/courses/abc, ?sort=BOGUS)
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String name = ex instanceof MethodArgumentTypeMismatchException argEx ? argEx.getName() : ex.getPropertyName();
        if (name == null) {
            return handleExceptionInternal(ex, null, headers, status, request);
        }
        String message = invalidValueMessage(name, ex.getRequiredType());
        return handleExceptionInternal(ex, invalidInput(List.of(new FieldError(name, message))), headers, status, request);
    }

    // 필수 쿼리 파라미터가 아예 안 왔을 때 (예: GET /api/auth/check-nickname 에 nickname 누락)
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message = "'" + ex.getParameterName() + "' 파라미터가 필요합니다";
        return handleExceptionInternal(ex, invalidInput(List.of(new FieldError(ex.getParameterName(), message))), headers, status, request);
    }

    // @RequestHeader로 필수 지정한 헤더가 아예 안 왔을 때 (예: logout의 Authorization 헤더 누락).
    // 폼 필드가 아니라서 errors는 채우지 않는다.
    @Override
    protected ResponseEntity<Object> handleServletRequestBindingException(
            ServletRequestBindingException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (ex instanceof MissingRequestHeaderException headerEx) {
            ApiResponse<Void> body = ApiResponse.error(GlobalErrorCode.INVALID_INPUT_VALUE, "'" + headerEx.getHeaderName() + "' 헤더가 필요합니다");
            return handleExceptionInternal(ex, body, headers, status, request);
        }
        return handleExceptionInternal(ex, null, headers, status, request);
    }

    // 부모가 처리하는 모든 표준 예외가 마지막에 모이는 곳. body가 없으면 상태 코드에 맞는 ErrorCode로 ApiResponse를 만든다.
    // Content-Type을 JSON으로 고정하는 이유: Accept가 application/xml처럼 JSON이 아니면 이 에러 응답도 같은 Accept 때문에
    // 직렬화에 실패해 빈 body가 나간다. Content-Type을 미리 지정하면 Spring이 Accept 협상을 건너뛴다 (406 대응).
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        BaseErrorCode fallbackCode = errorCodeOf(statusCode);
        if (statusCode.is5xxServerError()) {
            log.error("Spring MVC 처리 중 서버 오류", ex);
        } else if (!(body instanceof ApiResponse<?>) && fallbackCode.getHttpStatus().value() != statusCode.value()) {
            // 상태 코드와 code가 어긋나는 응답이 나가는 중 — 눈에 띄도록 error로 남긴다 (#131)
            log.error("매핑되지 않은 {} 응답: code가 {}({})로 나간다. GlobalErrorCode에 전용 code를 추가하고 errorCodeOf에 매핑할 것",
                    statusCode.value(), fallbackCode.getCode(), fallbackCode.getHttpStatus().value(), ex);
        } else {
            log.warn("{}: {}", ex.getClass().getSimpleName(), ex.getMessage());
        }

        Object responseBody = body instanceof ApiResponse<?> ? body : ApiResponse.error(fallbackCode);
        HttpHeaders jsonHeaders = HttpHeaders.copyOf(headers);
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
        return super.handleExceptionInternal(ex, responseBody, jsonHeaders, statusCode, request);
    }

    // ---------- helpers ----------

    // 표준 예외의 상태 코드 → 응답 code. 목록에 없는 4xx(예: 413 업로드 용량 초과)는 상태 코드는 그대로 두고
    // code만 INVALID_INPUT_VALUE(400)로 나가서 둘이 어긋난다. 지금은 도달 경로가 없고(업로드 기능 없음), 실제로 나가면
    // handleExceptionInternal이 error 로그를 남긴다. 그 4xx를 쓰는 기능 PR에서 전용 code를 추가해 여기 매핑한다 (#131).
    private static BaseErrorCode errorCodeOf(HttpStatusCode status) {
        return switch (status.value()) {
            case 404 -> GlobalErrorCode.API_NOT_FOUND;
            case 405 -> GlobalErrorCode.METHOD_NOT_ALLOWED;
            case 406 -> GlobalErrorCode.NOT_ACCEPTABLE;
            case 415 -> GlobalErrorCode.UNSUPPORTED_MEDIA_TYPE;
            default -> status.is5xxServerError() ? GlobalErrorCode.INTERNAL_SERVER_ERROR : GlobalErrorCode.INVALID_INPUT_VALUE;
        };
    }

    // 대표 message는 첫 번째 오류 문구, errors는 전체
    private static ApiResponse<Void> invalidInput(List<FieldError> errors) {
        String message = errors.isEmpty() ? GlobalErrorCode.INVALID_INPUT_VALUE.getMessage() : errors.get(0).message();
        return ApiResponse.error(GlobalErrorCode.INVALID_INPUT_VALUE, message, errors);
    }

    // enum이면 허용 값을 알려주고, 아니면 형식만 틀렸다고 알려준다
    private static String invalidValueMessage(String name, @Nullable Class<?> requiredType) {
        if (requiredType != null && requiredType.isEnum()) {
            String allowed = Arrays.stream(requiredType.getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            return "'" + name + "' 값은 " + allowed + " 중 하나여야 합니다";
        }
        return "'" + name + "' 값의 형식이 올바르지 않습니다";
    }

    // Jackson 경로 → "reviews[0].rating" 형태. 클래스명은 넣지 않는다.
    private static String jsonPath(List<JacksonException.Reference> path) {
        StringBuilder sb = new StringBuilder();
        for (JacksonException.Reference ref : path) {
            if (ref.getPropertyName() != null) {
                if (!sb.isEmpty()) {
                    sb.append('.');
                }
                sb.append(ref.getPropertyName());
            } else if (ref.getIndex() >= 0) {
                sb.append('[').append(ref.getIndex()).append(']');
            }
        }
        return sb.toString();
    }

    private static <T extends Throwable> @Nullable T findCause(Throwable e, Class<T> type) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (type.isInstance(t)) {
                return type.cast(t);
            }
        }
        return null;
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
}
