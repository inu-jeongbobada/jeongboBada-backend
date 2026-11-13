package com.inu.jeongbobada.global.file;

import com.inu.jeongbobada.global.exception.code.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum FileErrorCode implements BaseErrorCode {

    FILE_EMPTY(HttpStatus.BAD_REQUEST, "FILE_EMPTY", "파일을 첨부해주세요"), // 400
    // 기본 문구 대신 허용 형식을 넣은 문구로 던진다 (예: pdf, jpg 파일만 올릴 수 있어요)
    FILE_TYPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "FILE_TYPE_NOT_ALLOWED", "올릴 수 없는 파일 형식이에요"), // 400
    // 호출하는 쪽의 최대 크기를 넘었거나, 그보다 먼저 spring.servlet.multipart.max-file-size에 걸린 경우
    FILE_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "FILE_TOO_LARGE", "파일 용량이 너무 커요"), // 413
    FILE_UPLOAD_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "FILE_UPLOAD_FAILED", "파일을 올리지 못했어요. 잠시 후 다시 시도해주세요"), // 503
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    FileErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
