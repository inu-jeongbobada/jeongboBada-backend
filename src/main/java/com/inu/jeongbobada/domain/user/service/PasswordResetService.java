package com.inu.jeongbobada.domain.user.service;

import com.inu.jeongbobada.domain.user.dto.PasswordResetSendCodeResponse;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final String MAIL_SUBJECT = "[정보바다] 비밀번호 재설정 인증코드";
    private static final int MAX_DAILY_RESENDS = 5;
    // 서버 시간대가 UTC여도 재발송 횟수는 한국 자정에 초기화되도록 고정한다
    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationCodeManager codeManager;
    private final EmailSender emailSender;

    // 와이어프레임에 따라 학번·이메일 불일치를 구분해서 응답한다.
    // 횟수를 정확히 세려고 행을 잠근다. 메일 발송이 실패하면 예외로 롤백돼 코드와 횟수가 남지 않는다.
    @Transactional
    public PasswordResetSendCodeResponse sendCode(String studentId, String email) {
        String normalizedEmail = EmailNormalizer.normalize(email);
        User user = userRepository.findByStudentIdForUpdate(studentId)
            .filter(found -> normalizedEmail.equals(found.getEmail()))
            .orElseThrow(() -> new BusinessException(UserErrorCode.PASSWORD_RESET_ACCOUNT_NOT_FOUND));

        // 그날의 첫 발송은 재발송이 아니므로, 오늘 보낸 횟수가 곧 이번 발송의 재발송 번호다
        LocalDate today = LocalDate.now(SERVICE_ZONE);
        int resendCount = user.passwordResetSendCountOn(today);
        if (resendCount > MAX_DAILY_RESENDS) {
            throw new BusinessException(UserErrorCode.PASSWORD_RESET_RESEND_LIMIT_EXCEEDED);
        }

        LocalDateTime now = LocalDateTime.now();
        if (codeManager.isWithinResendCooldown(user.getPasswordResetCode(), now)) {
            throw new BusinessException(UserErrorCode.VERIFICATION_CODE_RESEND_TOO_FAST);
        }

        String code = codeManager.generateCode();
        user.updatePasswordResetCode(codeManager.issue(code, now));
        user.recordPasswordResetSend(today, resendCount + 1);

        try {
            emailSender.send(user.getEmail(), MAIL_SUBJECT, mailBody(code));
        } catch (Exception e) {
            log.error("비밀번호 재설정 메일 발송 실패", e);
            throw new BusinessException(UserErrorCode.EMAIL_SEND_FAILED);
        }
        return new PasswordResetSendCodeResponse(resendCount, MAX_DAILY_RESENDS);
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
