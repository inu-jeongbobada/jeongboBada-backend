package com.inu.jeongbobada.global.security;

import com.inu.jeongbobada.global.exception.code.GlobalErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAccessDeniedHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JwtAccessDeniedHandler accessDeniedHandler = new JwtAccessDeniedHandler(objectMapper);

    @Test
    void 권한이_부족한_요청은_403_JSON으로_응답한다() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        accessDeniedHandler.handle(request, response, new AccessDeniedException("권한 없음"));

        // then
        assertThat(response.getStatus()).isEqualTo(GlobalErrorCode.FORBIDDEN.getHttpStatus().value());
        // setCharacterEncoding 호출 순서상 charset이 뒤에 붙으므로 접두사만 확인
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("success").asBoolean()).isFalse();
        assertThat(body.get("code").asString()).isEqualTo(GlobalErrorCode.FORBIDDEN.getCode());
        assertThat(body.get("message").asString()).isEqualTo(GlobalErrorCode.FORBIDDEN.getMessage());
    }
}
