package com.inu.jeongbobada.domain.user.service;

import java.util.Locale;

// 이메일은 대소문자/공백 차이로 같은 주소가 중복 등록되거나 일치 비교에서 빗나가지 않도록
// 저장·비교 전에 항상 이 메서드로 정규화한다.
final class EmailNormalizer {

    private EmailNormalizer() {
    }

    static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
