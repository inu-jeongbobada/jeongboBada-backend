package com.inu.jeongbobada.domain.user.service;

import com.inu.jeongbobada.domain.user.entity.User;
import com.inu.jeongbobada.domain.user.entity.VerificationCode;
import com.inu.jeongbobada.domain.user.exception.UserErrorCode;
import com.inu.jeongbobada.domain.user.repository.UserRepository;
import com.inu.jeongbobada.global.exception.BusinessException;
import com.inu.jeongbobada.global.mail.EmailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordResetServiceTest {

    private static final String STUDENT_ID = "202012345";
    private static final String EMAIL = "user@example.com";

    private final UserRepository userRepository = mock(UserRepository.class);
    private final EmailSender emailSender = mock(EmailSender.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final VerificationCodeManager codeManager = new VerificationCodeManager(passwordEncoder);
    private final PasswordResetService service =
        new PasswordResetService(userRepository, passwordEncoder, codeManager, emailSender);

    private User user;

    @BeforeEach
    void setUp() {
        user = User.create(STUDENT_ID, passwordEncoder.encode("oldPassword1"), "테스터", null, EMAIL);
        when(userRepository.findByStudentIdForUpdate(STUDENT_ID)).thenReturn(Optional.of(user));
    }

    private void assertSendCodeFails(String studentId, String email, UserErrorCode expected) {
        assertThatThrownBy(() -> service.sendCode(studentId, email))
            .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }

    // 발송된 메일 본문에서 6자리 코드를 꺼낸다
    private String sentCode() {
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq(EMAIL), anyString(), body.capture());
        Matcher matcher = Pattern.compile("\\d{6}").matcher(body.getValue());
        assertThat(matcher.find()).isTrue();
        return matcher.group();
    }

    private String issueCodeAndGet() {
        service.sendCode(STUDENT_ID, EMAIL);
        return sentCode();
    }

    @Test
    void 코드를_요청하면_가입한_이메일로_발송되고_DB에는_해시만_저장된다() {
        service.sendCode(STUDENT_ID, EMAIL);

        String code = sentCode();
        assertThat(user.getPasswordResetCode().codeHash()).isNotEqualTo(code);
        assertThat(passwordEncoder.matches(code, user.getPasswordResetCode().codeHash())).isTrue();
    }

    @Test
    void 이메일은_대소문자와_공백이_달라도_같은_주소로_취급한다() {
        service.sendCode(STUDENT_ID, "  User@Example.COM ");

        verify(emailSender).send(eq(EMAIL), anyString(), anyString());
    }

    @Test
    void 없는_학번이면_메일을_보내지_않고_404로_구분한다() {
        when(userRepository.findByStudentIdForUpdate("999999999")).thenReturn(Optional.empty());

        assertSendCodeFails("999999999", EMAIL, UserErrorCode.PASSWORD_RESET_ACCOUNT_NOT_FOUND);

        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void 가입한_이메일과_다르면_메일을_보내지_않고_404로_구분한다() {
        assertSendCodeFails(STUDENT_ID, "other@gmail.com", UserErrorCode.PASSWORD_RESET_ACCOUNT_NOT_FOUND);

        verify(emailSender, never()).send(anyString(), anyString(), anyString());
        assertThat(user.getPasswordResetCode()).isNull();
    }

    @Test
    void 이메일이_등록되지_않은_계정은_메일을_보내지_않는다() {
        User noEmail = User.create(STUDENT_ID, passwordEncoder.encode("oldPassword1"), "테스터", null, null);
        when(userRepository.findByStudentIdForUpdate(STUDENT_ID)).thenReturn(Optional.of(noEmail));

        assertSendCodeFails(STUDENT_ID, EMAIL, UserErrorCode.PASSWORD_RESET_ACCOUNT_NOT_FOUND);

        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void 쿨다운_안에_다시_요청하면_429이고_메일을_또_보내지_않는다() {
        service.sendCode(STUDENT_ID, EMAIL);

        assertSendCodeFails(STUDENT_ID, EMAIL, UserErrorCode.VERIFICATION_CODE_RESEND_TOO_FAST);

        verify(emailSender, times(1)).send(anyString(), anyString(), anyString());
    }

    @Test
    void 메일_발송이_실패하면_503이다() {
        doThrow(new IllegalStateException("smtp down")).when(emailSender).send(anyString(), anyString(), anyString());

        assertSendCodeFails(STUDENT_ID, EMAIL, UserErrorCode.EMAIL_SEND_FAILED);
    }

    @Test
    void 첫_발송_뒤_재발송은_하루_5회까지이고_횟수를_알려준다() {
        for (int expected = 0; expected <= 5; expected++) {
            user.clearPasswordResetCode(); // 쿨다운을 건너뛰고 횟수 제한만 확인한다
            assertThat(service.sendCode(STUDENT_ID, EMAIL).resendCount()).isEqualTo(expected);
        }
        user.clearPasswordResetCode();

        assertSendCodeFails(STUDENT_ID, EMAIL, UserErrorCode.PASSWORD_RESET_RESEND_LIMIT_EXCEEDED);

        verify(emailSender, times(6)).send(anyString(), anyString(), anyString());
    }

    @Test
    void 날짜가_바뀌면_재발송_횟수가_초기화된다() {
        user.recordPasswordResetSend(LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1), 6);

        assertThat(service.sendCode(STUDENT_ID, EMAIL).resendCount()).isZero();
    }

    @Test
    void verify는_코드가_맞아도_소모하지_않는다() {
        String code = issueCodeAndGet();

        service.verifyCode(STUDENT_ID, code);
        service.verifyCode(STUDENT_ID, code);

        assertThat(user.getPasswordResetCode()).isNotNull();
    }

    @Test
    void 틀린_코드는_불일치_에러로_실패_횟수를_알려주고_횟수가_쌓인다() {
        String code = issueCodeAndGet();
        String wrong = code.equals("000000") ? "111111" : "000000";

        assertThatThrownBy(() -> service.verifyCode(STUDENT_ID, wrong))
            .isInstanceOfSatisfying(BusinessException.class, e -> {
                assertThat(e.getErrorCode()).isEqualTo(UserErrorCode.PASSWORD_RESET_CODE_MISMATCH);
                assertThat(e.getMessage()).contains("(1/5회)");
            });
        assertThat(user.getPasswordResetCode().failedAttempts()).isEqualTo(1);
    }

    @Test
    void 다섯_번_틀리면_코드가_폐기돼서_올바른_코드도_더는_통하지_않는다() {
        String code = issueCodeAndGet();
        String wrong = code.equals("000000") ? "111111" : "000000";

        for (int i = 0; i < VerificationCodeManager.MAX_FAILED_ATTEMPTS - 1; i++) {
            assertThatThrownBy(() -> service.verifyCode(STUDENT_ID, wrong))
                .isInstanceOfSatisfying(BusinessException.class,
                    e -> assertThat(e.getErrorCode()).isEqualTo(UserErrorCode.PASSWORD_RESET_CODE_MISMATCH));
        }
        assertThatThrownBy(() -> service.verifyCode(STUDENT_ID, wrong))
            .isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(UserErrorCode.PASSWORD_RESET_CODE_ATTEMPTS_EXCEEDED));

        assertThat(user.getPasswordResetCode()).isNull();
        assertThatThrownBy(() -> service.verifyCode(STUDENT_ID, code)).isInstanceOf(BusinessException.class);
    }

    @Test
    void 만료된_코드는_맞아도_실패한다() {
        String code = "123456";
        LocalDateTime issuedAt = LocalDateTime.now().minusMinutes(10);
        user.updatePasswordResetCode(VerificationCode.issue(passwordEncoder.encode(code), issuedAt, issuedAt.plusMinutes(5)));

        assertThatThrownBy(() -> service.verifyCode(STUDENT_ID, code))
            .isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(UserErrorCode.PASSWORD_RESET_CODE_EXPIRED));
        assertThat(user.getPasswordResetCode()).isNull();
    }

    @Test
    void 없는_학번은_코드가_없는_것과_같은_에러다() {
        when(userRepository.findByStudentIdForUpdate("999999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifyCode("999999999", "123456"))
            .isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(UserErrorCode.INVALID_VERIFICATION_CODE));
    }

    @Test
    void 재설정하면_비밀번호가_바뀌고_코드와_refresh_token이_폐기된다() {
        user.updateRefreshToken("refresh-token", LocalDateTime.now().plusDays(1));
        String code = issueCodeAndGet();

        service.resetPassword(STUDENT_ID, code, "newPassword1");

        assertThat(passwordEncoder.matches("newPassword1", user.getPassword())).isTrue();
        assertThat(passwordEncoder.matches("oldPassword1", user.getPassword())).isFalse();
        assertThat(user.getPasswordResetCode()).isNull();
        assertThat(user.getRefreshToken()).isNull();
    }

    @Test
    void 이전과_같은_비밀번호는_거부하고_코드는_유지해서_다른_비밀번호로_다시_시도할_수_있다() {
        user.updateRefreshToken("refresh-token", LocalDateTime.now().plusDays(1));
        String code = issueCodeAndGet();

        assertThatThrownBy(() -> service.resetPassword(STUDENT_ID, code, "oldPassword1"))
            .isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(UserErrorCode.PASSWORD_RESET_SAME_PASSWORD));
        assertThat(user.getPasswordResetCode().failedAttempts()).isZero();
        assertThat(user.getRefreshToken()).isEqualTo("refresh-token");

        service.resetPassword(STUDENT_ID, code, "newPassword1");
        assertThat(passwordEncoder.matches("newPassword1", user.getPassword())).isTrue();
    }

    @Test
    void 코드는_1회용이라_같은_코드로_두_번_재설정할_수_없다() {
        String code = issueCodeAndGet();
        service.resetPassword(STUDENT_ID, code, "newPassword1");

        assertThatThrownBy(() -> service.resetPassword(STUDENT_ID, code, "anotherPassword1"))
            .isInstanceOf(BusinessException.class);
        assertThat(passwordEncoder.matches("newPassword1", user.getPassword())).isTrue();
    }

    @Test
    void 틀린_코드로는_비밀번호가_바뀌지_않는다() {
        issueCodeAndGet();

        assertThatThrownBy(() -> service.resetPassword(STUDENT_ID, "000000", "newPassword1"))
            .isInstanceOf(BusinessException.class);
        assertThat(passwordEncoder.matches("oldPassword1", user.getPassword())).isTrue();
    }

    @Test
    void 이메일_변경용으로_받은_코드로는_비밀번호를_재설정할_수_없다() {
        // 로그인한 공격자가 자기 이메일로 이메일 변경 코드를 받아 비밀번호 재설정에 쓰는 우회를 막는지 확인
        String emailChangeCode = "654321";
        LocalDateTime now = LocalDateTime.now();
        user.requestEmailChange("attacker@example.com",
            VerificationCode.issue(passwordEncoder.encode(emailChangeCode), now, now.plusMinutes(5)));

        assertThatThrownBy(() -> service.resetPassword(STUDENT_ID, emailChangeCode, "hacked12345"))
            .isInstanceOf(BusinessException.class);
        assertThat(passwordEncoder.matches("oldPassword1", user.getPassword())).isTrue();
    }

    @Test
    void 코드를_요청하지_않았다면_어떤_코드로도_재설정할_수_없다() {
        verify(emailSender, never()).send(anyString(), anyString(), any());

        assertThatThrownBy(() -> service.resetPassword(STUDENT_ID, "123456", "newPassword1"))
            .isInstanceOf(BusinessException.class);
    }
}
