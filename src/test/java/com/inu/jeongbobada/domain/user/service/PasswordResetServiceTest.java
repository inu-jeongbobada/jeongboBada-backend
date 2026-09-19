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

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
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
        when(userRepository.findByStudentId(STUDENT_ID)).thenReturn(Optional.of(user));
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
    void 없는_학번이면_메일을_보내지_않고_예외도_내지_않는다() {
        when(userRepository.findByStudentId("999999999")).thenReturn(Optional.empty());

        assertThatCode(() -> service.sendCode("999999999", EMAIL)).doesNotThrowAnyException();

        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void 가입한_이메일과_다르면_메일을_보내지_않고_예외도_내지_않는다() {
        assertThatCode(() -> service.sendCode(STUDENT_ID, "other@gmail.com")).doesNotThrowAnyException();

        verify(emailSender, never()).send(anyString(), anyString(), anyString());
        assertThat(user.getPasswordResetCode()).isNull();
    }

    @Test
    void 이메일이_등록되지_않은_계정은_메일을_보내지_않는다() {
        User noEmail = User.create(STUDENT_ID, passwordEncoder.encode("oldPassword1"), "테스터", null, null);
        when(userRepository.findByStudentId(STUDENT_ID)).thenReturn(Optional.of(noEmail));

        assertThatCode(() -> service.sendCode(STUDENT_ID, EMAIL)).doesNotThrowAnyException();

        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void 쿨다운_안에_다시_요청하면_메일을_또_보내지_않는다() {
        service.sendCode(STUDENT_ID, EMAIL);
        service.sendCode(STUDENT_ID, EMAIL);

        verify(emailSender, times(1)).send(anyString(), anyString(), anyString());
    }

    @Test
    void 메일_발송이_실패해도_예외를_내지_않고_코드를_폐기한다() {
        doThrow(new IllegalStateException("smtp down")).when(emailSender).send(anyString(), anyString(), anyString());

        assertThatCode(() -> service.sendCode(STUDENT_ID, EMAIL)).doesNotThrowAnyException();

        assertThat(user.getPasswordResetCode()).isNull();
    }

    @Test
    void verify는_코드가_맞아도_소모하지_않는다() {
        String code = issueCodeAndGet();

        service.verifyCode(STUDENT_ID, code);
        service.verifyCode(STUDENT_ID, code);

        assertThat(user.getPasswordResetCode()).isNotNull();
    }

    @Test
    void 틀린_코드는_INVALID_VERIFICATION_CODE이고_실패_횟수가_쌓인다() {
        issueCodeAndGet();

        assertThatThrownBy(() -> service.verifyCode(STUDENT_ID, "000000"))
            .isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(UserErrorCode.INVALID_VERIFICATION_CODE));
        assertThat(user.getPasswordResetCode().failedAttempts()).isEqualTo(1);
    }

    @Test
    void 다섯_번_틀리면_코드가_폐기돼서_올바른_코드도_더는_통하지_않는다() {
        String code = issueCodeAndGet();
        String wrong = code.equals("000000") ? "111111" : "000000";

        for (int i = 0; i < VerificationCodeManager.MAX_FAILED_ATTEMPTS; i++) {
            assertThatThrownBy(() -> service.verifyCode(STUDENT_ID, wrong)).isInstanceOf(BusinessException.class);
        }

        assertThat(user.getPasswordResetCode()).isNull();
        assertThatThrownBy(() -> service.verifyCode(STUDENT_ID, code)).isInstanceOf(BusinessException.class);
    }

    @Test
    void 만료된_코드는_맞아도_실패한다() {
        String code = "123456";
        LocalDateTime issuedAt = LocalDateTime.now().minusMinutes(10);
        user.updatePasswordResetCode(VerificationCode.issue(passwordEncoder.encode(code), issuedAt, issuedAt.plusMinutes(5)));

        assertThatThrownBy(() -> service.verifyCode(STUDENT_ID, code)).isInstanceOf(BusinessException.class);
    }

    @Test
    void 없는_학번은_코드가_틀린_것과_같은_에러다() {
        when(userRepository.findByStudentId("999999999")).thenReturn(Optional.empty());

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
