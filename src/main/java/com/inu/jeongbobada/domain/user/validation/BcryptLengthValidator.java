package com.inu.jeongbobada.domain.user.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

public class BcryptLengthValidator implements ConstraintValidator<BcryptLength, String> {

    private static final int BCRYPT_MAX_BYTES = 72;

    // null은 @NotBlank가 잡는다
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || value.getBytes(StandardCharsets.UTF_8).length <= BCRYPT_MAX_BYTES;
    }
}
