package com.inu.jeongbobada.domain.user.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// BCrypt는 72바이트를 넘는 비밀번호를 암호화하지 못하고 예외를 던진다.
// @Size는 글자 수만 세므로 한글처럼 여러 바이트인 문자가 많으면 통과한 뒤 500이 된다.
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BcryptLengthValidator.class)
public @interface BcryptLength {

    String message() default "비밀번호가 너무 깁니다. 영문·숫자는 1바이트, 한글은 3바이트로 계산해 72바이트 이하여야 합니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
