package com.inu.jeongbobada.global.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// #109: 요청 형식이 잘못된 클라이언트 요청이 500이 아니라 알맞은 4xx + ApiResponse 형식으로 나가는지.
// 예외가 실제 DispatcherServlet → GlobalExceptionHandler 경로를 타야 의미가 있어서 통합 테스트로 확인한다.
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class RequestFormatErrorIntegrationTest {

    private static final String LOGIN_JSON = """
        {"studentId":"999999999","password":"password1"}
        """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void JSON_API에_text_plain으로_보내면_415() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.TEXT_PLAIN)
                .content(LOGIN_JSON))
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void JSON_API에_Content_Type_없이_보내면_415() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .content(LOGIN_JSON))
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void JSON으로_응답할_수_없는_Accept면_406을_JSON_형식으로() throws Exception {
        mockMvc.perform(get("/api/courses")
                .accept(MediaType.APPLICATION_XML))
            .andExpect(status().isNotAcceptable())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.code").value("NOT_ACCEPTABLE"));
    }

    @Test
    void 필수_쿼리_파라미터가_없으면_400() throws Exception {
        mockMvc.perform(get("/api/auth/check-nickname"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }
}
