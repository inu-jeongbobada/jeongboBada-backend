package com.inu.jeongbobada.domain.user.service;

import com.inu.jeongbobada.domain.user.dto.LoginRequest;
import com.inu.jeongbobada.domain.user.dto.SignupRequest;
import com.inu.jeongbobada.domain.user.dto.TokenResponse;
import com.inu.jeongbobada.domain.user.entity.User;
import com.inu.jeongbobada.domain.user.exception.UserErrorCode;
import com.inu.jeongbobada.domain.user.repository.UserRepository;
import com.inu.jeongbobada.global.exception.BusinessException;
import com.inu.jeongbobada.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

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

    // 학번+비밀번호 검증을 AuthenticationManager에 위임
    // 내부적으로 StudentUserDetailsService(학번 조회) + passwordEncoder(비밀번호 대조)를 스프링이 알아서 씀
    // 실패하면 BadCredentialsException 발생 -> GlobalExceptionHandler에서 처리
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.studentId(), request.password())
        );

        // 검증 통과 -> 토큰 발급
        String accessToken = jwtTokenProvider.createAccessToken(request.studentId());

        return new TokenResponse(accessToken, null); // refreshToken은 후속 작업에서 채울 예정
    }


}


