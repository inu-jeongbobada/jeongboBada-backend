package com.inu.jeongbobada.global.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// #111: 400 응답이 "어느 필드가 왜 잘못됐는지"를 알려주는지. 프론트가 폼 칸마다 오류를 표시하려면
// message 문구만으로는 부족해서 errors[{field, message}]를 함께 내려준다.
// 실제 요청이 DispatcherServlet → GlobalExceptionHandler를 거쳐야 의미가 있어서 통합 테스트로 확인한다.
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ValidationErrorFieldsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 검증_실패는_실패한_필드를_모두_errors로_내려준다() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
            .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder("studentId", "password", "nickname", "email")))
            .andExpect(jsonPath("$.errors[?(@.field == 'studentId')].message").value("학번은 필수입니다."));
    }

    @Test
    void 잘못된_enum_값은_필드와_허용_값을_알려준다() throws Exception {
        mockMvc.perform(post("/api/courses/1/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"rating":"SIX"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
            .andExpect(jsonPath("$.errors[0].field").value("rating"))
            .andExpect(jsonPath("$.message").value(containsString("ONE, TWO, THREE, FOUR, FIVE")));
    }

    @Test
    void 깨진_JSON은_내부_클래스명_없이_안내한다() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"studentId\":"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(allOf(
                containsString("JSON"),
                not(containsString("com.inu")),
                not(containsString("jackson")))));
    }

    @Test
    void 경로_변수_타입이_틀리면_파라미터_이름을_알려준다() throws Exception {
        mockMvc.perform(get("/api/courses/abc"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("courseId"))
            .andExpect(jsonPath("$.message").value(containsString("courseId")));
    }

    @Test
    void 쿼리_enum_값이_틀리면_허용_값을_알려준다() throws Exception {
        mockMvc.perform(get("/api/courses/1/reviews").param("sort", "BOGUS"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("sort"))
            .andExpect(jsonPath("$.message").value(containsString("RECOMMENDED, RATING, LATEST")));
    }

    @Test
    void 필수_파라미터가_없으면_파라미터_이름을_알려준다() throws Exception {
        mockMvc.perform(get("/api/auth/check-nickname"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("nickname"))
            .andExpect(jsonPath("$.message").value(containsString("nickname")));
    }

    @Test
    void 검증과_무관한_에러에는_errors가_없다() throws Exception {
        mockMvc.perform(get("/api/courses").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist());

        mockMvc.perform(post("/api/courses"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
            .andExpect(jsonPath("$.errors").doesNotExist());
    }
}
