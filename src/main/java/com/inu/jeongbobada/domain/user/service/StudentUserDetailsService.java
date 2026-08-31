package com.inu.jeongbobada.domain.user.service;

import com.inu.jeongbobada.domain.user.entity.User;
import com.inu.jeongbobada.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
//Spring Security에는 이미 UserDetailsService라는 인터페이스가 있다.
public class StudentUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String studentId) {
        User user = userRepository.findByStudentId(studentId)
            //아래 줄을 전역 예외처리로 해여할거 같은데
            .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 학번입니다"));

        //DB 사용자를 Spring Security용 형식으로 번역한다? 정도로 이해
        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getStudentId())
            .password(user.getPassword())
            .authorities("ROLE_" + user.getUserRole().name())
            .build();
    }
}
