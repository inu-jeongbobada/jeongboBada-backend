package com.inu.jeongbobada.global.security;

import tools.jackson.databind.ObjectMapper;
import com.inu.jeongbobada.global.common.ApiResponse;
import com.inu.jeongbobada.global.exception.code.GlobalErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

// 인증이 필요한 경로에 토큰 없이(또는 무효한 토큰으로) 접근했을 때 Spring Security가 호출해주는 콜백.
// 기본 동작은 로그인 페이지로 리다이렉트인데, REST API라 401 JSON으로 바꿔서 응답한다.
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException
    ) throws IOException {
        ApiResponse<Void> body = ApiResponse.error(GlobalErrorCode.UNAUTHORIZED);

        response.setStatus(body.httpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
