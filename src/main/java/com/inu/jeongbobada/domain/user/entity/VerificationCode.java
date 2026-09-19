package com.inu.jeongbobada.domain.user.entity;

import jakarta.persistence.Embeddable;

import java.time.LocalDateTime;

// 이메일 인증코드 1건의 상태. 코드 원문은 저장하지 않고 해시만 저장한다.
// USERS 테이블에 직접 컬럼으로 들어가며(별도 테이블/Redis 없음), 용도별 컬럼명은 User에서 @AttributeOverride로 지정한다.
// 불변 객체라 실패 횟수를 올릴 때는 새 객체로 교체한다.
@Embeddable
public record VerificationCode(
    String codeHash,
    LocalDateTime issuedAt,
    LocalDateTime expiresAt,
    Integer failedAttempts
) {

    public VerificationCode {
        if (failedAttempts == null) {
            failedAttempts = 0;
        }
    }

    public static VerificationCode issue(String codeHash, LocalDateTime issuedAt, LocalDateTime expiresAt) {
        return new VerificationCode(codeHash, issuedAt, expiresAt, 0);
    }

    public boolean isExpired(LocalDateTime now) {
        return !now.isBefore(expiresAt);
    }

    public VerificationCode withFailedAttempt() {
        return new VerificationCode(codeHash, issuedAt, expiresAt, failedAttempts + 1);
    }
}
