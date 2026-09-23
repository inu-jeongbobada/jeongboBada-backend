package com.inu.jeongbobada.domain.user.exception;

import com.inu.jeongbobada.global.exception.code.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum UserErrorCode implements BaseErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "존재하지 않는 사용자입니다"), // 404
    DUPLICATE_STUDENT_ID(HttpStatus.CONFLICT, "DUPLICATE_STUDENT_ID", "이미 가입된 학번입니다"), // 409
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "DUPLICATE_NICKNAME", "이미 사용 중인 닉네임입니다"), // 409
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "학번 또는 비밀번호가 일치하지 않습니다"), // 401
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "유효하지 않거나 만료된 Refresh Token입니다"), // 401
    CURRENT_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "CURRENT_PASSWORD_MISMATCH", "현재 비밀번호가 일치하지 않습니다"), // 400 (401이면 프론트의 토큰 재발급 인터셉터와 충돌)
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "이미 사용 중인 이메일입니다"), // 409
    // 코드가 틀렸는지/만료됐는지/애초에 없는지를 구분해서 알려주지 않는다 (추측 공격에 힌트를 주지 않기 위함)
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "INVALID_VERIFICATION_CODE", "인증코드가 올바르지 않거나 만료되었습니다"), // 400
    SAME_EMAIL(HttpStatus.BAD_REQUEST, "SAME_EMAIL", "현재 사용 중인 이메일과 동일합니다"), // 400
    VERIFICATION_CODE_RESEND_TOO_FAST(HttpStatus.TOO_MANY_REQUESTS, "VERIFICATION_CODE_RESEND_TOO_FAST", "인증코드는 잠시 후 다시 요청할 수 있습니다"), // 429
    EMAIL_SEND_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "EMAIL_SEND_FAILED", "이메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요"), // 503
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
