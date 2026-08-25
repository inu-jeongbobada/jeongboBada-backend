package com.inu.jeongbobada.domain.user.entity;

import com.inu.jeongbobada.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Column(name = "NICKNAME", nullable = false, unique = true, length = 50)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "ROLE", nullable = false, length = 20)
    private UserRole userRole;

    @Column(name = "DEPARTMENT", length = 100)
    private String department;

    public User(String studentId, String password, String nickname, UserRole userRole, String department) {
        this.studentId = studentId;
        this.password = password;
        this.nickname = nickname;
        this.userRole = userRole;
        this.department = department;
    }
}
