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

    // 비밀번호 찾기용 이메일. 회원가입 요청에서는 필수지만, DB 컬럼은 null 허용 —
    // 이 기능 추가 전에 만들어진 행이 있는 DB(팀원 로컬 등)에서 ddl-auto: update가 컬럼을 못 추가하는 걸 막기 위함.
    // unique는 null이 여러 개여도 통과한다. 항상 소문자/trim으로 정규화해서 저장한다.
    // studentId처럼 응답 DTO 노출 금지 대상 (민감정보 취급)
    @Column(name = "EMAIL", unique = true, length = 100)
    private String email;

    @Builder(access = AccessLevel.PRIVATE)
    private User(String studentId, String password, String nickname, UserRole userRole, String department, String email) {
        this.studentId = studentId;
        this.password = password;
        this.nickname = nickname;
        this.userRole = userRole;
        this.department = department;
        this.email = email;
    }

    // 비밀번호 찾기 / 이메일 변경 인증코드는 용도별로 컬럼을 분리한다.
    // 하나를 공유하면 "이메일 변경용으로 받은 코드"로 비밀번호를 재설정하는 우회가 생기기 때문.
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "codeHash", column = @Column(name = "PASSWORD_RESET_CODE_HASH", length = 100)),
        @AttributeOverride(name = "issuedAt", column = @Column(name = "PASSWORD_RESET_CODE_ISSUED_AT")),
        @AttributeOverride(name = "expiresAt", column = @Column(name = "PASSWORD_RESET_CODE_EXPIRES_AT")),
        @AttributeOverride(name = "failedAttempts", column = @Column(name = "PASSWORD_RESET_CODE_FAILED_ATTEMPTS"))
    })
    private VerificationCode passwordResetCode;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "codeHash", column = @Column(name = "EMAIL_CHANGE_CODE_HASH", length = 100)),
        @AttributeOverride(name = "issuedAt", column = @Column(name = "EMAIL_CHANGE_CODE_ISSUED_AT")),
        @AttributeOverride(name = "expiresAt", column = @Column(name = "EMAIL_CHANGE_CODE_EXPIRES_AT")),
        @AttributeOverride(name = "failedAttempts", column = @Column(name = "EMAIL_CHANGE_CODE_FAILED_ATTEMPTS"))
    })
    private VerificationCode emailChangeCode;

    // 인증코드를 발송한 "새 이메일". 코드 검증을 통과해도 요청한 새 이메일과 같아야 변경된다.
    @Column(name = "PENDING_EMAIL", length = 100)
    private String pendingEmail;

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

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    // null을 넘기면 코드를 폐기한다 (만료·한도 초과·사용 완료)
    public void updatePasswordResetCode(VerificationCode code) {
        this.passwordResetCode = code;
    }

    public void clearPasswordResetCode() {
        this.passwordResetCode = null;
    }

    // 새 이메일로 인증코드를 발송할 때: 어떤 이메일로 보냈는지(pendingEmail)도 함께 기록
    public void requestEmailChange(String pendingEmail, VerificationCode code) {
        this.pendingEmail = pendingEmail;
        this.emailChangeCode = code;
    }

    public void updateEmailChangeCode(VerificationCode code) {
        this.emailChangeCode = code;
    }

    public void clearEmailChange() {
        this.pendingEmail = null;
        this.emailChangeCode = null;
    }

    // 이메일 변경 확정 — 사용한 코드와 대기 중이던 새 이메일은 함께 소모
    public void changeEmail(String email) {
        this.email = email;
        clearEmailChange();
    }

    public static User create(String studentId, String encodedPassword, String nickname, String department, String email) {
        return User.builder()
            .studentId(studentId)
            .password(encodedPassword)
            .nickname(nickname)
            .userRole(UserRole.USER)
            .department(department)
            .email(email)
            .build();
    }
}
