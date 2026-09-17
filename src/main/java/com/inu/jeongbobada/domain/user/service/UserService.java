package com.inu.jeongbobada.domain.user.service;

import com.inu.jeongbobada.domain.user.entity.User;
import com.inu.jeongbobada.domain.user.exception.UserErrorCode;
import com.inu.jeongbobada.domain.user.repository.UserRepository;
import com.inu.jeongbobada.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

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

    private User getUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
    }
}
