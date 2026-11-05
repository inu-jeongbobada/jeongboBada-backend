package com.inu.jeongbobada.domain.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inu.jeongbobada.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// BCrypt는 72바이트를 넘는 비밀번호를 암호화하지 못한다. 글자 수 검증(64자)만으로는 한글 비밀번호가 통과해 500이 됐다.
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class PasswordByteLimitIntegrationTest {

    private static final String PASSWORD = "password1";
    // 26자, 2 + 23*3 + 1 = 72바이트
    private static final String PASSWORD_72_BYTES = "a1" + "가".repeat(23) + "b";
    // 27자, 2 + 23*3 + 2 = 73바이트
    private static final String PASSWORD_73_BYTES = "a1" + "가".repeat(23) + "bc";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String studentId;
    private int seed;

    @BeforeEach
    void pickStudentId() {
        seed = ThreadLocalRandom.current().nextInt(100_000_000);
        studentId = String.format("9%08d", seed);
    }

    @AfterEach
    void cleanUp() {
        userRepository.findByStudentId(studentId).ifPresent(userRepository::delete);
    }

    @Test
    void 비밀번호_72바이트는_가입된다() throws Exception {
        signUp(PASSWORD_72_BYTES).andExpect(status().isCreated());

        assertThat(PASSWORD_72_BYTES.getBytes(StandardCharsets.UTF_8)).hasSize(72);
    }

    @Test
    void 비밀번호_73바이트로_가입하면_400이다() throws Exception {
        signUp(PASSWORD_73_BYTES)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
            .andExpect(jsonPath("$.errors[0].field").value("password"));

        assertThat(PASSWORD_73_BYTES.getBytes(StandardCharsets.UTF_8)).hasSize(73);
    }

    @Test
    void 새_비밀번호_73바이트로_변경하면_400이다() throws Exception {
        signUp(PASSWORD).andExpect(status().isCreated());

        mockMvc.perform(patch("/api/users/me/password")
                .header("Authorization", "Bearer " + login(PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"currentPassword":"%s","newPassword":"%s"}
                    """.formatted(PASSWORD, PASSWORD_73_BYTES)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
            .andExpect(jsonPath("$.errors[0].field").value("newPassword"));
    }

    @Test
    void 새_비밀번호_73바이트로_재설정하면_400이다() throws Exception {
        mockMvc.perform(post("/api/auth/password-reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"studentId":"%s","code":"123456","newPassword":"%s"}
                    """.formatted(studentId, PASSWORD_73_BYTES)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
            .andExpect(jsonPath("$.errors[0].field").value("newPassword"));
    }

    @Test
    void 로그인에_72바이트를_넘는_비밀번호를_보내도_500이_아니라_401이다() throws Exception {
        signUp(PASSWORD).andExpect(status().isCreated());

        requestLogin(PASSWORD_73_BYTES)
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    private ResultActions signUp(String password) throws Exception {
        return mockMvc.perform(post("/api/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"studentId":"%s","password":"%s","nickname":"pw%d","email":"pw%d@example.com"}
                """.formatted(studentId, password, seed % 10_000, seed)));
    }

    private String login(String password) throws Exception {
        String body = requestLogin(password)
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body).path("data").path("accessToken").asText();
    }

    private ResultActions requestLogin(String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"studentId":"%s","password":"%s"}
                """.formatted(studentId, password)));
    }
}
