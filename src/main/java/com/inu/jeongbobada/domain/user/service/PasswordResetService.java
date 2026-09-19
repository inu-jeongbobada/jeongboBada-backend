package com.inu.jeongbobada.domain.user.service;

import com.inu.jeongbobada.domain.user.entity.User;
import com.inu.jeongbobada.domain.user.exception.UserErrorCode;
import com.inu.jeongbobada.domain.user.repository.UserRepository;
import com.inu.jeongbobada.global.exception.BusinessException;
import com.inu.jeongbobada.global.mail.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final String MAIL_SUBJECT = "[정보바다] 비밀번호 재설정 인증코드";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationCodeManager codeManager;
    private final EmailSender emailSender;

    // 학번이 없거나, 가입 때 등록한 이메일과 다르거나, 메일 발송이 실패해도 호출자에게는 항상 똑같이 성공으로 보인다.
    // 응답이 다르면 "이 학번은 가입돼 있다 / 이 이메일이 맞다"를 알아낼 수 있어서(user enumeration) 로그인 API와 같은 원칙을 따른다.
    // 원인은 서버 로그로만 남긴다 (학번/이메일/코드는 로그에 남기지 않는다).
    @Transactional
    public void sendCode(String studentId, String email) {
        String normalizedEmail = EmailNormalizer.normalize(email);
        Optional<User> found = userRepository.findByStudentId(studentId)
            .filter(user -> normalizedEmail.equals(user.getEmail()));

        if (found.isEmpty()) {
            log.info("비밀번호 재설정 코드 요청 무시: 학번 또는 이메일 불일치");
            return;
        }

        User user = found.get();
        LocalDateTime now = LocalDateTime.now();
        if (codeManager.isWithinResendCooldown(user.getPasswordResetCode(), now)) {
            log.info("비밀번호 재설정 코드 요청 무시: 재발송 쿨다운 중");
            return;
        }

        String code = codeManager.generateCode();
        user.updatePasswordResetCode(codeManager.issue(code, now));

        try {
            emailSender.send(user.getEmail(), MAIL_SUBJECT, mailBody(code));
        } catch (Exception e) {
            // 못 받은 코드가 쿨다운으로 재요청을 막지 않도록 폐기
            log.error("비밀번호 재설정 메일 발송 실패", e);
            user.clearPasswordResetCode();
        }
    }

    // 확인만 하고 코드는 소모하지 않는다 (화면에서 다음 단계로 넘어가도 되는지 판단하는 용도).
    // 서버가 "검증 통과"를 기억하지 않으므로, 실제 재설정 API가 코드를 다시 검증한다.
    // 틀린 횟수가 예외 때문에 롤백되면 무제한 시도가 가능해지므로 BusinessException에도 커밋한다.
    @Transactional(noRollbackFor = BusinessException.class)
    public void verifyCode(String studentId, String code) {
        checkCode(studentId, code);
    }

    // 코드를 검증하고 비밀번호를 바꾼 뒤 코드를 폐기한다 (1회용).
    // 비밀번호 변경 API와 동일하게 기존 refresh token도 무효화해서 다른 기기는 재로그인하게 한다.
    @Transactional(noRollbackFor = BusinessException.class)
    public void resetPassword(String studentId, String code, String newPassword) {
        User user = checkCode(studentId, code);

        user.updatePassword(passwordEncoder.encode(newPassword));
        user.clearPasswordResetCode();
        user.clearRefreshToken();
    }

    // 없는 학번 / 코드 없음 / 만료 / 불일치를 모두 같은 에러로 응답
    private User checkCode(String studentId, String code) {
        User user = userRepository.findByStudentId(studentId)
            .orElseThrow(() -> new BusinessException(UserErrorCode.INVALID_VERIFICATION_CODE));

        VerificationCodeManager.Verification result =
            codeManager.verify(user.getPasswordResetCode(), code, LocalDateTime.now());
        user.updatePasswordResetCode(result.next());

        if (!result.matched()) {
            throw new BusinessException(UserErrorCode.INVALID_VERIFICATION_CODE);
        }
        return user;
    }

    private String mailBody(String code) {
        return "비밀번호 재설정 인증코드: " + code + "\n\n"
            + "인증코드는 " + VerificationCodeManager.CODE_TTL.toMinutes() + "분 동안만 유효합니다.\n"
            + "본인이 요청하지 않았다면 이 메일을 무시해주세요.";
    }
}
