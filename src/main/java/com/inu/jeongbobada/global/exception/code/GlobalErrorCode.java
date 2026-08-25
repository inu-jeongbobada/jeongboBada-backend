package com.inu.jeongbobada.global.exception.code;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum GlobalErrorCode implements BaseErrorCode {

    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON_400", "잘못된 입력값입니다"), // 400
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_404", "요청하신 경로를 찾을 수 없습니다"), // 404
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON_405", "지원하지 않는 HTTP 메서드입니다"), // 405
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "서버 내부 오류가 발생했습니다"), // 500
    ;

    private final HttpStatus httpStatus;
    // code를 enum 상수 이름(name())으로 대체하지 않고 별도 필드로 둠.
    // name()을 그대로 쓰면 나중에 상수 이름을 리팩터링(가독성 개선 등)할 때마다
    // 프론트로 나가는 응답 코드 문자열까지 같이 바뀌어버려서, API 계약이 자바 변수명에 묶이게 된다.
    // code 필드를 따로 두면 자바 코드 리팩터링과 API 계약을 분리할 수 있다.
    // 기현-생각
    private final String code;
    private final String message;

    GlobalErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
