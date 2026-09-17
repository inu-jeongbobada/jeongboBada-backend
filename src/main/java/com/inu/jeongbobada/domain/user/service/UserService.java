package com.inu.jeongbobada.domain.user.service;

import com.inu.jeongbobada.domain.user.entity.User;
import com.inu.jeongbobada.domain.user.exception.UserErrorCode;
import com.inu.jeongbobada.domain.user.repository.UserRepository;
import com.inu.jeongbobada.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 본인 닉네임과 동일한 값으로의 "변경"은 중복 검사에서 걸리지 않게 자기 자신은 제외하고 확인
    @Transactional
    public void updateNickname(Long userId, String nickname) {
        User user = getUser(userId);

        userRepository.findByNickname(nickname)
            .filter(owner -> !owner.getUserId().equals(userId))
            .ifPresent(owner -> {
                throw new BusinessException(UserErrorCode.DUPLICATE_NICKNAME);
            });

        user.updateNickname(nickname);
    }

    // 비밀번호 변경 성공 시 다른 기기/세션에 남아있던 refresh token은 무효화(재로그인 필요)
    @Transactional
    public void updatePassword(Long userId, String currentPassword, String newPassword) {
        User user = getUser(userId);

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BusinessException(UserErrorCode.PASSWORD_MISMATCH);
        }

        user.updatePassword(passwordEncoder.encode(newPassword));
        user.clearRefreshToken();
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
    }
}
