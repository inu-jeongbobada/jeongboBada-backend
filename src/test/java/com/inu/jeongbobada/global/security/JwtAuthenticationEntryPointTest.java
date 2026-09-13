package com.inu.jeongbobada.global.security;

import com.inu.jeongbobada.global.exception.code.GlobalErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationEntryPointTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint(objectMapper);

    @Test
    void 인증되지_않은_요청은_401_JSON으로_응답한다() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        entryPoint.commence(request, response, new BadCredentialsException("인증 실패"));

        // then
        assertThat(response.getStatus()).isEqualTo(GlobalErrorCode.UNAUTHORIZED.getHttpStatus().value());
        // setCharacterEncoding 호출 순서상 charset이 뒤에 붙으므로 접두사만 확인
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("success").asBoolean()).isFalse();
        assertThat(body.get("code").asString()).isEqualTo(GlobalErrorCode.UNAUTHORIZED.getCode());
        assertThat(body.get("message").asString()).isEqualTo(GlobalErrorCode.UNAUTHORIZED.getMessage());
    }
}
