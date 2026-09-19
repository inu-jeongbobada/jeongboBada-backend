package com.inu.jeongbobada.domain.user.service;

import com.inu.jeongbobada.domain.user.entity.VerificationCode;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class VerificationCodeManagerTest {

    private final VerificationCodeManager manager = new VerificationCodeManager(new BCryptPasswordEncoder());
    private final LocalDateTime now = LocalDateTime.of(2026, 9, 19, 12, 0, 0);

    @Test
    void 생성한_코드는_항상_숫자_6자리다() {
        for (int i = 0; i < 200; i++) {
            assertThat(manager.generateCode()).matches("\\d{6}");
        }
    }

    @Test
    void 발급하면_원문이_아니라_해시가_저장되고_5분_뒤_만료된다() {
        VerificationCode issued = manager.issue("123456", now);

        assertThat(issued.codeHash()).isNotEqualTo("123456");
        assertThat(issued.issuedAt()).isEqualTo(now);
        assertThat(issued.expiresAt()).isEqualTo(now.plusMinutes(5));
        assertThat(issued.failedAttempts()).isZero();
    }

    @Test
    void 재발송_쿨다운_60초_안에는_true_지나면_false_코드가_없으면_false() {
        VerificationCode issued = manager.issue("123456", now);

        assertThat(manager.isWithinResendCooldown(issued, now.plusSeconds(59))).isTrue();
        assertThat(manager.isWithinResendCooldown(issued, now.plusSeconds(60))).isFalse();
        assertThat(manager.isWithinResendCooldown(null, now)).isFalse();
    }

    @Test
    void 올바른_코드면_일치하고_코드는_그대로_유지된다() {
        VerificationCode issued = manager.issue("123456", now);

        VerificationCodeManager.Verification result = manager.verify(issued, "123456", now.plusMinutes(1));

        assertThat(result.matched()).isTrue();
        assertThat(result.next()).isEqualTo(issued);
    }

    @Test
    void 틀린_코드면_실패_횟수만_올라가고_코드는_유지된다() {
        VerificationCode issued = manager.issue("123456", now);

        VerificationCodeManager.Verification result = manager.verify(issued, "000000", now.plusMinutes(1));

        assertThat(result.matched()).isFalse();
        assertThat(result.next().failedAttempts()).isEqualTo(1);
    }

    @Test
    void 다섯_번째_실패에서_코드가_폐기된다() {
        VerificationCode code = manager.issue("123456", now);

        for (int i = 1; i < VerificationCodeManager.MAX_FAILED_ATTEMPTS; i++) {
            code = manager.verify(code, "000000", now.plusMinutes(1)).next();
            assertThat(code).isNotNull();
        }
        VerificationCodeManager.Verification last = manager.verify(code, "000000", now.plusMinutes(1));

        assertThat(last.matched()).isFalse();
        assertThat(last.next()).isNull();
    }

    @Test
    void 만료된_코드는_맞아도_실패하고_폐기된다() {
        VerificationCode issued = manager.issue("123456", now);

        VerificationCodeManager.Verification result = manager.verify(issued, "123456", now.plusMinutes(5));

        assertThat(result.matched()).isFalse();
        assertThat(result.next()).isNull();
    }

    @Test
    void 발급된_코드가_없으면_실패한다() {
        VerificationCodeManager.Verification result = manager.verify(null, "123456", now);

        assertThat(result.matched()).isFalse();
        assertThat(result.next()).isNull();
    }
}
