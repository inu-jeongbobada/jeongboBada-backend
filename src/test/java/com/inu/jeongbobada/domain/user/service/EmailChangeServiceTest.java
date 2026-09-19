package com.inu.jeongbobada.domain.user.service;

import com.inu.jeongbobada.domain.user.entity.User;
import com.inu.jeongbobada.domain.user.exception.UserErrorCode;
import com.inu.jeongbobada.domain.user.repository.UserRepository;
import com.inu.jeongbobada.global.exception.BusinessException;
import com.inu.jeongbobada.global.mail.EmailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailChangeServiceTest {

    private static final Long USER_ID = 1L;
    private static final String OLD_EMAIL = "old@gmail.com";
    private static final String NEW_EMAIL = "new@gmail.com";
    private static final String PASSWORD = "password1";

    private final UserRepository userRepository = mock(UserRepository.class);
    private final EmailSender emailSender = mock(EmailSender.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final VerificationCodeManager codeManager = new VerificationCodeManager(passwordEncoder);
    private final EmailChangeService service =
        new EmailChangeService(userRepository, passwordEncoder, codeManager, emailSender);

    private User user;

    @BeforeEach
    void setUp() {
        user = User.create("202012345", passwordEncoder.encode(PASSWORD), "테스터", null, OLD_EMAIL);
        ReflectionTestUtils.setField(user, "userId", USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
    }

    private String sentCodeTo(String email) {
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq(email), anyString(), body.capture());
        Matcher matcher = Pattern.compile("\\d{6}").matcher(body.getValue());
        assertThat(matcher.find()).isTrue();
        return matcher.group();
    }

    private static void assertErrorCode(Throwable thrown, UserErrorCode expected) {
        assertThat(thrown).isInstanceOfSatisfying(BusinessException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }

    @Test
    void 인증코드는_현재_이메일이_아니라_새_이메일로_발송된다() {
        service.sendCode(USER_ID, NEW_EMAIL);

        sentCodeTo(NEW_EMAIL);
        verify(emailSender, never()).send(eq(OLD_EMAIL), anyString(), anyString());
        assertThat(user.getPendingEmail()).isEqualTo(NEW_EMAIL);
        // 발송만으로는 이메일이 바뀌지 않는다
        assertThat(user.getEmail()).isEqualTo(OLD_EMAIL);
    }

    @Test
    void 현재_이메일과_같으면_거부한다() {
        assertThatThrownBy(() -> service.sendCode(USER_ID, " OLD@gmail.com "))
            .satisfies(e -> assertErrorCode(e, UserErrorCode.SAME_EMAIL));
    }

    @Test
    void 이미_다른_사람이_쓰는_이메일이면_거부한다() {
        when(userRepository.findByEmail(NEW_EMAIL)).thenReturn(Optional.of(mock(User.class)));

        assertThatThrownBy(() -> service.sendCode(USER_ID, NEW_EMAIL))
            .satisfies(e -> assertErrorCode(e, UserErrorCode.DUPLICATE_EMAIL));
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void 쿨다운_안에_다시_요청하면_거부한다() {
        service.sendCode(USER_ID, NEW_EMAIL);

        assertThatThrownBy(() -> service.sendCode(USER_ID, NEW_EMAIL))
            .satisfies(e -> assertErrorCode(e, UserErrorCode.CODE_RESEND_TOO_FAST));
        verify(emailSender, times(1)).send(anyString(), anyString(), anyString());
    }

    @Test
    void 메일_발송이_실패하면_EMAIL_SEND_FAILED로_응답한다() {
        doThrow(new IllegalStateException("smtp down")).when(emailSender).send(anyString(), anyString(), anyString());

        assertThatThrownBy(() -> service.sendCode(USER_ID, NEW_EMAIL))
            .satisfies(e -> assertErrorCode(e, UserErrorCode.EMAIL_SEND_FAILED));
    }

    @Test
    void 코드와_현재_비밀번호가_맞으면_이메일이_바뀌고_코드는_소모된다() {
        service.sendCode(USER_ID, NEW_EMAIL);
        String code = sentCodeTo(NEW_EMAIL);

        service.updateEmail(USER_ID, NEW_EMAIL, code, PASSWORD);

        assertThat(user.getEmail()).isEqualTo(NEW_EMAIL);
        assertThat(user.getEmailChangeCode()).isNull();
        assertThat(user.getPendingEmail()).isNull();
    }

    @Test
    void 현재_비밀번호가_틀리면_코드가_맞아도_바뀌지_않는다() {
        service.sendCode(USER_ID, NEW_EMAIL);
        String code = sentCodeTo(NEW_EMAIL);

        assertThatThrownBy(() -> service.updateEmail(USER_ID, NEW_EMAIL, code, "wrongPassword1"))
            .satisfies(e -> assertErrorCode(e, UserErrorCode.PASSWORD_MISMATCH));
        assertThat(user.getEmail()).isEqualTo(OLD_EMAIL);
    }

    @Test
    void 코드를_받은_이메일과_다른_주소로는_바꿀_수_없다() {
        service.sendCode(USER_ID, NEW_EMAIL);
        String code = sentCodeTo(NEW_EMAIL);

        assertThatThrownBy(() -> service.updateEmail(USER_ID, "attacker@example.com", code, PASSWORD))
            .satisfies(e -> assertErrorCode(e, UserErrorCode.INVALID_VERIFICATION_CODE));
        assertThat(user.getEmail()).isEqualTo(OLD_EMAIL);
    }

    @Test
    void 틀린_코드는_실패하고_다섯_번_틀리면_폐기된다() {
        service.sendCode(USER_ID, NEW_EMAIL);
        String code = sentCodeTo(NEW_EMAIL);
        String wrong = code.equals("000000") ? "111111" : "000000";

        for (int i = 0; i < VerificationCodeManager.MAX_FAILED_ATTEMPTS; i++) {
            assertThatThrownBy(() -> service.updateEmail(USER_ID, NEW_EMAIL, wrong, PASSWORD))
                .satisfies(e -> assertErrorCode(e, UserErrorCode.INVALID_VERIFICATION_CODE));
        }

        assertThat(user.getEmailChangeCode()).isNull();
        assertThat(user.getPendingEmail()).isNull();
        // 이미 폐기됐으므로 올바른 코드도 통하지 않는다
        assertThatThrownBy(() -> service.updateEmail(USER_ID, NEW_EMAIL, code, PASSWORD))
            .isInstanceOf(BusinessException.class);
        assertThat(user.getEmail()).isEqualTo(OLD_EMAIL);
    }

    @Test
    void 코드를_받은_뒤_다른_사람이_같은_이메일을_먼저_등록했으면_거부한다() {
        service.sendCode(USER_ID, NEW_EMAIL);
        String code = sentCodeTo(NEW_EMAIL);
        User other = mock(User.class);
        when(other.getUserId()).thenReturn(2L);
        when(userRepository.findByEmail(NEW_EMAIL)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.updateEmail(USER_ID, NEW_EMAIL, code, PASSWORD))
            .satisfies(e -> assertErrorCode(e, UserErrorCode.DUPLICATE_EMAIL));
        assertThat(user.getEmail()).isEqualTo(OLD_EMAIL);
    }

    @Test
    void 이메일이_없던_기존_가입자도_같은_방식으로_처음_등록할_수_있다() {
        User legacy = User.create("202012346", passwordEncoder.encode(PASSWORD), "기존", null, null);
        ReflectionTestUtils.setField(legacy, "userId", 3L);
        when(userRepository.findById(3L)).thenReturn(Optional.of(legacy));

        service.sendCode(3L, NEW_EMAIL);
        String code = sentCodeTo(NEW_EMAIL);
        service.updateEmail(3L, NEW_EMAIL, code, PASSWORD);

        assertThat(legacy.getEmail()).isEqualTo(NEW_EMAIL);
    }
}
