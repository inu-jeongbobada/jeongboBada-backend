package com.inu.jeongbobada.domain.user.service;

import com.inu.jeongbobada.domain.user.dto.LoginRequest;
import com.inu.jeongbobada.domain.user.dto.NicknameCheckResponse;
import com.inu.jeongbobada.domain.user.dto.ReissueRequest;
import com.inu.jeongbobada.domain.user.dto.SignupRequest;
import com.inu.jeongbobada.domain.user.dto.TokenResponse;
import com.inu.jeongbobada.domain.user.entity.User;
import com.inu.jeongbobada.domain.user.exception.UserErrorCode;
import com.inu.jeongbobada.domain.user.repository.UserRepository;
import com.inu.jeongbobada.global.exception.BusinessException;
import com.inu.jeongbobada.global.security.JwtProperties;
import com.inu.jeongbobada.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    // 학번/닉네임 중복 확인 후 비밀번호 암호화해서 저장
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
        //이메일 중복확인 (비밀번호 찾기에서 이메일로 계정을 특정하므로 unique)
        String email = EmailNormalizer.normalize(request.email());
        if (userRepository.findByEmail(email).isPresent()) {
            throw new BusinessException(UserErrorCode.DUPLICATE_EMAIL);
        }
        //DB에 저장할때 해쉬값으로 바꿔서 저장
        String encodedPassword = passwordEncoder.encode(request.password());

        User user = User.create(
            request.studentId(),
            encodedPassword,
            request.nickname(),
            null, // department: 회원가입 요청에 아직 없는 값이라 일단 null
            email
        );

        userRepository.save(user);

    }

    // 가입 폼에서 제출 전 미리 확인하는 용도. 최종 중복 검사는 signup()에서 다시 함(여기서 통과해도 그 사이 다른 사람이 선점할 수 있음)
    @Transactional(readOnly = true)
    public NicknameCheckResponse checkNicknameAvailability(String nickname) {
        return new NicknameCheckResponse(!userRepository.existsByNickname(nickname));
    }

    // 학번+비밀번호 검증(AuthenticationManager에 위임) 통과하면 access/refresh 토큰 새로 발급
    @Transactional
    public TokenResponse login(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.studentId(), request.password())
        );

        User user = userRepository.findByStudentId(request.studentId())
            .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        return issueTokens(user);
    }

    // 클라이언트가 보낸 refresh token을 검증(서명/만료 + DB 저장값 일치)하고, 통과하면 토큰 재발급
    @Transactional
    public TokenResponse reissue(ReissueRequest request) {
        String refreshToken = request.refreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(UserErrorCode.INVALID_REFRESH_TOKEN);
        }

        String studentId = jwtTokenProvider.getStudentId(refreshToken);
        User user = userRepository.findByStudentId(studentId)
            .orElseThrow(() -> new BusinessException(UserErrorCode.INVALID_REFRESH_TOKEN));

        // DB에 저장된 값과 일치하는지, 만료 안 됐는지 확인
        if (user.getRefreshToken() == null
            || !user.getRefreshToken().equals(refreshToken)
            || user.getRefreshTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(UserErrorCode.INVALID_REFRESH_TOKEN);
        }

        return issueTokens(user);
    }

    // 로그아웃: refresh token(권장) 또는 access token(예전 방식)으로 사용자를 찾아 DB의 refresh token을 제거(무효화)
    @Transactional
    public void logout(@Nullable String refreshToken, @Nullable String accessToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            revokeByRefreshToken(refreshToken);
            return;
        }
        if (accessToken == null || !jwtTokenProvider.validateToken(accessToken)) {
            return; // 만료·무효한 access token이면 사용자를 특정할 수 없어 조용히 종료 (refresh 방식을 권장하는 이유)
        }
        String studentId = jwtTokenProvider.getStudentId(accessToken);
        userRepository.findByStudentId(studentId)
            .ifPresent(User::clearRefreshToken);
    }

    // refresh token으로 로그아웃: access token 만료 여부와 상관없이 지운다 (#129).
    // DB에 저장된 값과 같을 때만 지운다 — 이미 교체된 예전 refresh로 요청하면, 그 사이 새로 로그인한
    // 다른 기기의 현재 refresh까지 지워버리는 일을 막기 위해서다.
    private void revokeByRefreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            return; // 만료·위조된 refresh는 어차피 재발급에 쓸 수 없다
        }
        String studentId = jwtTokenProvider.getStudentId(refreshToken);
        userRepository.findByStudentId(studentId)
            .filter(user -> refreshToken.equals(user.getRefreshToken()))
            .ifPresent(User::clearRefreshToken);
    }

    // access/refresh 토큰을 새로 만들고, refresh token은 만료시각과 함께 유저 엔티티에 저장
    private TokenResponse issueTokens(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getStudentId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getStudentId());

        LocalDateTime expiresAt = LocalDateTime.now()
            .plus(Duration.ofMillis(jwtProperties.refreshExpiration()));
        user.updateRefreshToken(refreshToken, expiresAt);

        return new TokenResponse(accessToken, refreshToken);
    }
}


