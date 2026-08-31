package com.inu.jeongbobada.domain.user.service;

import com.inu.jeongbobada.domain.user.dto.SignupRequest;
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
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void signup (SignupRequest request) {
        //학번 중복 확인
        if (userRepository.findByStudentId(request.studentId()).isPresent()) {
            throw new BusinessException(UserErrorCode.DUPLICATE_STUDENT_ID);
        }
        //닉네임 중복확인
        if (userRepository.findByNickname(request.nickname()).isPresent()) {
            throw new BusinessException(UserErrorCode.DUPLICATE_NICKNAME);
        }

        //DB에 저장할때 해쉬값으로 바꿔서 저장
        String encodedPassword = passwordEncoder.encode(request.password());

        User user = User.create(
            request.studentId(),
            encodedPassword,
            request.nickname(),
            null // department: 회원가입 요청에 아직 없는 값이라 일단 null
        );

        userRepository.save(user);

    }
}


