package com.inu.jeongbobada.domain.user.service;

import com.inu.jeongbobada.domain.user.entity.VerificationCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

// 비밀번호 찾기 / 이메일 변경이 공유하는 인증코드 정책(생성·만료·재발송 쿨다운·틀림 횟수 제한)
@Component
@RequiredArgsConstructor
public class VerificationCodeManager {

    public static final Duration CODE_TTL = Duration.ofMinutes(5);
    public static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    public static final int MAX_FAILED_ATTEMPTS = 5;

    private static final int CODE_BOUND = 1_000_000; // 6자리

    private final SecureRandom secureRandom = new SecureRandom();
    private final PasswordEncoder passwordEncoder;

    public String generateCode() {
        return String.format("%06d", secureRandom.nextInt(CODE_BOUND));
    }

    // DB에는 해시만 저장 (비밀번호와 동일하게 BCrypt)
    public VerificationCode issue(String rawCode, LocalDateTime now) {
        return VerificationCode.issue(passwordEncoder.encode(rawCode), now, now.plus(CODE_TTL));
    }

    // 직전 발급 후 쿨다운이 지나지 않았으면 true — 메일 폭탄 방지
    public boolean isWithinResendCooldown(VerificationCode current, LocalDateTime now) {
        return current != null && now.isBefore(current.issuedAt().plus(RESEND_COOLDOWN));
    }

    // next: 호출한 쪽이 User에 저장해야 할 다음 상태 (null이면 코드를 폐기)
    //  - 일치: 그대로 유지 (소모는 호출한 쪽이 결정 — verify-code는 유지, 실제 변경 시점에 소모)
    //  - 불일치: 실패 횟수 +1, 한도에 도달하면 폐기
    //  - 없음/만료/이미 한도 초과: 폐기
    public Verification verify(VerificationCode stored, String rawCode, LocalDateTime now) {
        if (stored == null || stored.isExpired(now) || stored.failedAttempts() >= MAX_FAILED_ATTEMPTS) {
            return new Verification(false, null);
        }
        if (passwordEncoder.matches(rawCode, stored.codeHash())) {
            return new Verification(true, stored);
        }
        VerificationCode failed = stored.withFailedAttempt();
        return new Verification(false, failed.failedAttempts() >= MAX_FAILED_ATTEMPTS ? null : failed);
    }

    public record Verification(boolean matched, VerificationCode next) {
    }
}
