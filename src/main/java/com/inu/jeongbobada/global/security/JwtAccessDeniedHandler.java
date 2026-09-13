package com.inu.jeongbobada.global.security;

import tools.jackson.databind.ObjectMapper;
import com.inu.jeongbobada.global.common.ApiResponse;
import com.inu.jeongbobada.global.exception.code.GlobalErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

// 인증은 됐지만 권한(예: ADMIN)이 부족한 요청에 접근했을 때 Spring Security가 호출해주는 콜백.
// @PreAuthorize("hasRole('ADMIN')")로 막힌 API를 USER 권한으로 호출하면 여기로 들어옴.
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        AccessDeniedException accessDeniedException
    ) throws IOException {
        ApiResponse<Void> body = ApiResponse.error(GlobalErrorCode.FORBIDDEN);

        response.setStatus(body.httpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
