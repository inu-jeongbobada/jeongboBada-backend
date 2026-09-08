package com.inu.jeongbobada.global.security;

import com.inu.jeongbobada.domain.user.service.StudentUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// 매 요청마다 한 번씩 실행되는 필터 (OncePerRequestFilter는 Spring이 제공하는 베이스 클래스).
// 컨트롤러에 도달하기 전에, 토큰을 검사해서 SecurityContextHolder에 "이 요청은 누구다"를 채워 넣는 역할.
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final StudentUserDetailsService studentUserDetailsService;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveToken(request);

        // 토큰이 있고, 서명/만료가 유효할 때만 인증 처리. 없거나 무효하면 그냥 통과시킴
        // (여기서 401을 던지지 않음 — permitAll 경로는 토큰 없이도 통과해야 하고,
        //  "인증 안 됐는데 인증 필요한 경로 접근"에 대한 401 응답은 JwtAuthenticationEntryPoint가 담당)
        if (token != null && jwtTokenProvider.validateToken(token)) {
            String studentId = jwtTokenProvider.getStudentId(token);
            UserDetails userDetails = studentUserDetailsService.loadUserByUsername(studentId);

            // "이 사람이 인증됐다"를 표현하는 Spring Security 기본 객체.
            // 비밀번호 자리에 null을 넣는 이유: 이미 JWT 서명으로 검증이 끝났으니
            // 비밀번호를 다시 대조할 필요가 없어서.
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            // 여기 저장해야 SecurityConfig의 authenticated() 체크, @AuthenticationPrincipal 둘 다 동작함
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 인증 성공/실패와 무관하게 항상 호출 — 안 하면 요청이 다음 단계로 안 넘어가고 멈춤
        filterChain.doFilter(request, response);
    }

    // "Authorization: Bearer xxx.yyy.zzz" 헤더에서 접두사를 떼고 순수 토큰 문자열만 추출
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER_NAME);
        if (header != null && header.startsWith(TOKEN_PREFIX)) {
            return header.substring(TOKEN_PREFIX.length());
        }
        return null;
    }
}
