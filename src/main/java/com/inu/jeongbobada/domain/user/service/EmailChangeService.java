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

// 이메일은 비밀번호 찾기의 열쇠라서, 로그인 상태여도 값만 바꾸게 두면 계정 탈취 경로가 된다
// (access token 탈취 → 공격자 이메일로 변경 → 비밀번호 찾기로 재설정). 그래서 2단계로 나눈다.
//  1) 새 이메일로 인증코드 발송  2) 코드 + 현재 비밀번호를 확인하고 변경
// 이미 로그인한 사용자 대상이라 비밀번호 찾기와 달리 실패 사유를 그대로 알려준다.
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailChangeService {

    private static final String MAIL_SUBJECT = "[정보바다] 이메일 변경 인증코드";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationCodeManager codeManager;
    private final EmailSender emailSender;

    // 이메일이 아직 없는 기존 가입자가 처음 등록할 때도 같은 API를 쓴다.
    // 발송에 실패하면 예외로 트랜잭션이 롤백돼서 코드도 저장되지 않는다.
    @Transactional
    public void sendCode(Long userId, String newEmail) {
        User user = getUser(userId);
        String normalizedEmail = EmailNormalizer.normalize(newEmail);

        if (normalizedEmail.equals(user.getEmail())) {
            throw new BusinessException(UserErrorCode.SAME_EMAIL);
        }
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new BusinessException(UserErrorCode.DUPLICATE_EMAIL);
        }

        LocalDateTime now = LocalDateTime.now();
        if (codeManager.isWithinResendCooldown(user.getEmailChangeCode(), now)) {
            throw new BusinessException(UserErrorCode.CODE_RESEND_TOO_FAST);
        }

        String code = codeManager.generateCode();
        user.requestEmailChange(normalizedEmail, codeManager.issue(code, now));

        try {
            emailSender.send(normalizedEmail, MAIL_SUBJECT, mailBody(code));
        } catch (Exception e) {
            log.error("이메일 변경 인증 메일 발송 실패", e);
            throw new BusinessException(UserErrorCode.EMAIL_SEND_FAILED);
        }
    }

    // 틀린 횟수가 예외 때문에 롤백되면 무제한 시도가 가능해지므로 BusinessException에도 커밋한다.
    @Transactional(noRollbackFor = BusinessException.class)
    public void updateEmail(Long userId, String newEmail, String code, String currentPassword) {
        User user = getUser(userId);

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BusinessException(UserErrorCode.PASSWORD_MISMATCH);
        }

        // 코드를 받은 이메일과 다른 주소로 바꾸려는 요청은 코드 시도로 세지 않고 바로 거부
        String normalizedEmail = EmailNormalizer.normalize(newEmail);
        if (!normalizedEmail.equals(user.getPendingEmail())) {
            throw new BusinessException(UserErrorCode.INVALID_VERIFICATION_CODE);
        }

        VerificationCodeManager.Verification result =
            codeManager.verify(user.getEmailChangeCode(), code, LocalDateTime.now());
        if (result.next() == null) {
            user.clearEmailChange();
        } else {
            user.updateEmailChangeCode(result.next());
        }
        if (!result.matched()) {
            throw new BusinessException(UserErrorCode.INVALID_VERIFICATION_CODE);
        }

        // 코드를 받은 뒤 다른 사람이 같은 이메일을 먼저 등록했을 수 있으므로 한 번 더 확인
        userRepository.findByEmail(normalizedEmail)
            .filter(owner -> !owner.getUserId().equals(userId))
            .ifPresent(owner -> {
                throw new BusinessException(UserErrorCode.DUPLICATE_EMAIL);
            });

        user.changeEmail(normalizedEmail);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
    }

    private String mailBody(String code) {
        return "이메일 변경 인증코드: " + code + "\n\n"
            + "인증코드는 " + VerificationCodeManager.CODE_TTL.toMinutes() + "분 동안만 유효합니다.\n"
            + "본인이 요청하지 않았다면 이 메일을 무시해주세요.";
    }
}
