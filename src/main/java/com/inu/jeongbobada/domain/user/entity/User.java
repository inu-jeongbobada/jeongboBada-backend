package com.inu.jeongbobada.domain.user.entity;

import com.inu.jeongbobada.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "USERS") // USER -> SQL에서 예약어일 가능성 존재
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "USER_ID")
    private Long userId;

    // nullable -> DB에서 null X, unique -> DB에서 동일한 값 X, length -> DB에 저장될 최대 길이
    @Column(name = "STUDENT_ID", nullable = false, unique = true, length = 20)
    private String studentId;

    @Column(name = "PASSWORD", nullable = false)
    private String password;

    @Column(name = "NICKNAME", nullable = false, unique = true, length = 10)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name =  "ROLE", nullable = false, length = 20)
    private UserRole userRole;

    @Column(name = "DEPARTMENT", length = 100)
    private String department;

    @Builder(access = AccessLevel.PRIVATE)
    private User(String studentId, String password, String nickname, UserRole userRole, String department) {
        this.studentId = studentId;
        this.password = password;
        this.nickname = nickname;
        this.userRole = userRole;
        this.department = department;
    }

    // 별도 테이블/Redis 없이 USERS 테이블에 직접 저장 (멀티 디바이스 로그인 요구사항 없음)
    // studentId처럼 응답 DTO 외 노출 금지 대상 (민감정보 취급)
    @Column(name = "REFRESH_TOKEN", length = 500)
    private String refreshToken;

    @Column(name = "REFRESH_TOKEN_EXPIRES_AT")
    private LocalDateTime refreshTokenExpiresAt;

    // 로그인/재발급 시 새 refresh token으로 교체
    public void updateRefreshToken(String refreshToken, LocalDateTime expiresAt) {
        this.refreshToken = refreshToken;
        this.refreshTokenExpiresAt = expiresAt;
    }

    // 로그아웃 시 저장된 refresh token을 지워 재발급(reissue)에 못 쓰게 무효화
    public void clearRefreshToken() {
        this.refreshToken = null;
        this.refreshTokenExpiresAt = null;
    }

    public static User create(String studentId, String encodedPassword, String nickname, String department) {
        return User.builder()
            .studentId(studentId)
            .password(encodedPassword)
            .nickname(nickname)
            .userRole(UserRole.USER)
            .department(department)
            .build();
    }
}
