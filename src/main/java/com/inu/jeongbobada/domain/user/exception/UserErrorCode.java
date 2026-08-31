package com.inu.jeongbobada.domain.user.exception;

import com.inu.jeongbobada.global.exception.code.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum UserErrorCode implements BaseErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_404", "존재하지 않는 사용자입니다"), // 404
    DUPLICATE_STUDENT_ID(HttpStatus.CONFLICT, "USER_409", "이미 가입된 학번입니다"), // 409
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "USER_409", "이미 사용 중인 닉네임입니다"), // 409
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "USER_401", "학번 또는 비밀번호가 일치하지 않습니다"), // 401
    ;

    private final HttpStatus httpStatus;
    // code를 enum 상수 이름(name())으로 대체하지 않고 별도 필드로 둠.
    // name()을 그대로 쓰면 나중에 상수 이름을 리팩터링(가독성 개선 등)할 때마다
    // 프론트로 나가는 응답 코드 문자열까지 같이 바뀌어버려서, API 계약이 자바 변수명에 묶이게 된다.
    // code 필드를 따로 두면 자바 코드 리팩터링과 API 계약을 분리할 수 있다.
    private final String code;
    private final String message;

    UserErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
