package com.inu.jeongbobada.global.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// #83: SecurityConfig에 CORS 설정이 없어 프론트(React, 다른 origin)의 브라우저 요청이 막히던 문제.
// application-test.properties의 cors.allowed-origins=http://localhost:5173 를 기준으로 검증한다.
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class CorsConfigTest {

    private static final String ALLOWED_ORIGIN = "http://localhost:5173";
    private static final String DISALLOWED_ORIGIN = "https://not-allowed.example.com";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 허용된_origin의_preflight_요청은_200과_함께_Allow_Origin_헤더를_받는다() throws Exception {
        mockMvc.perform(options("/api/courses")
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name()))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN));
    }

    @Test
    void 허용되지_않은_origin의_preflight_요청은_Allow_Origin_헤더가_없다() throws Exception {
        mockMvc.perform(options("/api/courses")
                .header(HttpHeaders.ORIGIN, DISALLOWED_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name()))
            .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void 허용된_origin에서_실제_요청도_Allow_Origin_헤더를_받는다() throws Exception {
        mockMvc.perform(get("/api/courses").header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN));
    }

    @Test
    void Origin_헤더가_없는_서버간_호출은_CORS와_무관하게_그대로_동작한다() throws Exception {
        mockMvc.perform(get("/api/courses"))
            .andExpect(status().isOk())
            .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
