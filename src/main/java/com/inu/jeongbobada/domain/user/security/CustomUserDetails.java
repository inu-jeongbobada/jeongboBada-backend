package com.inu.jeongbobada.domain.user.security;

import com.inu.jeongbobada.domain.user.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

// JwtAuthenticationFilter가 SecurityContext에 채워 넣는 인증 주체(principal).
// 컨트롤러에서 @AuthenticationPrincipal CustomUserDetails로 꺼내 userId 등을 바로 씀.
public class CustomUserDetails implements UserDetails {

    private final Long userId;
    private final String studentId;
    private final String password;
    private final String nickname;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(User user) {
        this.userId = user.getUserId();
        this.studentId = user.getStudentId();
        this.password = user.getPassword();
        this.nickname = user.getNickname();
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getUserRole().name()));
    }

    // 아래부터는 UserDetails 인터페이스가 아닌, 우리가 필요해서 추가한 메서드 (선택)
    public Long getUserId() {
        return userId;
    }

    public String getNickname() {
        return nickname;
    }

    // UserDetails 필수 구현 메서드 (여기부터 3개, 없으면 컴파일 에러남)
    @Override
    public String getUsername() {
        return studentId;
    }

    // UserDetails 필수 구현 메서드
    @Override
    public String getPassword() {
        return password;
    }

    // UserDetails 필수 구현 메서드
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }
}
